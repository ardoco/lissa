/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.fuchss.restredis.client.Client;
import org.fuchss.restredis.client.ClientConfiguration;
import org.fuchss.restredis.server.Server;
import org.fuchss.restredis.server.ServerConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheParameter;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import kong.unirest.core.Unirest;

/**
 * Integration test for the REST Redis interface, using a Testcontainers-managed Redis instance.
 */
@Testcontainers
public class RestRedisTest {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    RestRedisCache<ClassifierCacheKey> restCache;
    private final ClassifierCacheParameter cacheParameter = new ClassifierCacheParameter("test", 1, 0.0);

    private static Path envFile;
    private static Thread serverThread;
    private static Client client;

    @TempDir
    private static Path tempCacheDir;

    @BeforeAll
    static void startServer() throws Exception {
        int httpPort = findFreePort();
        Path configFile = tempCacheDir.resolve("server_config.json");
        new ObjectMapper()
                .writeValue(
                        configFile.toFile(),
                        new ServerConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379), httpPort));

        serverThread = new Thread(
                () -> {
                    try {
                        Server.main(new String[] {configFile.toString()});
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                "rest-redis-test-server");
        serverThread.setDaemon(true);
        serverThread.start();

        String baseUrl = "http://localhost:" + httpPort;
        waitForServerReady(baseUrl);
        envFile = tempCacheDir.resolve(".env-rest");

        Files.writeString(envFile, """
                REST_REDIS_URI=%s
                REST_REDIS_USERNAME=
                REST_REDIS_PASSWORD=
                """.formatted(baseUrl));

        Environment.overwrite(envFile);
        client = new Client(new ClientConfiguration(baseUrl, null, null));
    }

    @AfterAll
    static void stopServer() throws InterruptedException {
        if (client != null) {
            client.close();
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread.join(5000);
        }
    }

    @BeforeEach
    public void setup() {
        Environment.overwrite(envFile);
        restCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());
    }

    /**
     * Tests that a connection to the redis client can be established.
     */
    @Test
    @DisplayName("Test REST Redis client connection")
    void testRestRedisConnection() {
        Cache.createByType(
                CacheType.REST_REDIS, new ClassifierCacheParameter("test", 1, 0.0), null, new ObjectMapper());
    }

    /**
     * Tests that the REST Redis cache can successfully set and get values, and that it returns null for non-existing keys.
     */
    @Test
    @DisplayName("Test REST Redis cache set and get")
    void testRestRedisCacheSetAndGet() {
        restCache.put("key", "value");
        String value = restCache.get("key", String.class);
        assertEquals("value", value);
        String nonExistingValue = restCache.get("ajhosadljhjyhxcjkhljysdhjk", String.class);
        assertNull(nonExistingValue);
    }

    /**
     * Tests that the hierarchical cache correctly handles conflicts between a local file cache and a REST Redis cache
     * when using the NONE strategy, ensuring that the primary cache value is returned and the secondary cache remains
     * unchanged.
     */
    @Test
    @DisplayName("Test HierarchicalCache with local and REST Redis cache")
    void testHierarchicalCacheWithLocalAndRestRedis() {
        // Given: Create local and REST Redis caches
        Cache<ClassifierCacheKey> localCache =
                new LocalCache<>(tempCacheDir.resolve("hierarchical_test.json").toString(), cacheParameter);
        Cache<ClassifierCacheKey> redisCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());

        // When: Direct writes to separate caches create a conflict
        String testKey = "conflict-key";
        String localValue = "local-value";
        String redisValue = "redis-value";

        localCache.put(testKey, localValue);
        redisCache.put(testKey, redisValue);

        // Create hierarchical cache with NONE strategy (returns primary value, backfills missing)
        HierarchicalCache<ClassifierCacheKey> hierarchicalCacheNone =
                new HierarchicalCache<>(cacheParameter, localCache, redisCache, CacheReplacementStrategy.NONE);

        // Then: NONE strategy returns primary (local) value
        String result = hierarchicalCacheNone.get(testKey, String.class);
        assertEquals(localValue, result);

        // And: Secondary cache remains unchanged
        assertEquals(redisValue, redisCache.get(testKey, String.class));
    }

    /**
     * Tests that the overwrite strategy correctly overwrites the secondary REST Redis cache with the primary local
     * cache value when there is a conflict.
     */
    @Test
    @DisplayName("Test HierarchicalCache OVERWRITE strategy with REST Redis")
    void testHierarchicalCacheOverwriteStrategyWithRestRedis() {
        // Given: Create local and REST Redis caches
        Cache<ClassifierCacheKey> localCache =
                new LocalCache<>(tempCacheDir.resolve("overwrite_test.json").toString(), cacheParameter);
        Cache<ClassifierCacheKey> redisCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());

        // When: Cache layers have conflicting values
        String testKey = "overwrite-key";
        String primaryValue = "primary-value";
        String secondaryValue = "secondary-value";

        localCache.put(testKey, primaryValue);
        redisCache.put(testKey, secondaryValue);

        // Create hierarchical cache with OVERWRITE strategy
        HierarchicalCache<ClassifierCacheKey> hierarchicalCacheOverwrite =
                new HierarchicalCache<>(cacheParameter, localCache, redisCache, CacheReplacementStrategy.OVERWRITE);

        // When: Get the value through hierarchical cache
        String result = hierarchicalCacheOverwrite.get(testKey, String.class);

        // Then: Primary value is returned
        assertEquals(primaryValue, result);

        // And: Secondary (REST Redis) cache is overwritten with primary value
        hierarchicalCacheOverwrite.flush();
        assertEquals(primaryValue, redisCache.get(testKey, String.class));
    }

    /**
     * Tests the error strategy for conflicting values in the remote REST cache and local file cache.
     */
    @Test
    @DisplayName("Test HierarchicalCache ERROR strategy detects conflicts with REST Redis")
    void testHierarchicalCacheErrorStrategyWithRestRedis() {
        // Given: Create local and REST Redis caches
        Cache<ClassifierCacheKey> localCache =
                new LocalCache<>(tempCacheDir.resolve("error_test.json").toString(), cacheParameter);
        Cache<ClassifierCacheKey> redisCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());

        // When: Cache layers have conflicting values
        String testKey = "error-key";
        String localValue = "local-value";
        String redisValue = "different-redis-value";

        localCache.put(testKey, localValue);
        redisCache.put(testKey, redisValue);

        // Create hierarchical cache with ERROR strategy
        HierarchicalCache<ClassifierCacheKey> hierarchicalCacheError =
                new HierarchicalCache<>(cacheParameter, localCache, redisCache, CacheReplacementStrategy.ERROR);

        // Then: Getting conflicting values throws an exception
        assertTrue(Assertions.assertThrows(
                        IllegalStateException.class, () -> hierarchicalCacheError.get(testKey, String.class))
                .getMessage()
                .contains("Cache inconsistency"));
    }

    /**
     * Tests backfilling from REST Redis to local cache when primary cache is missing a value.
     */
    @Test
    @DisplayName("Test HierarchicalCache backfill with REST Redis cache")
    void testHierarchicalCacheBackfillWithRestRedis() {
        // Given: Create local and REST Redis caches
        Cache<ClassifierCacheKey> localCache =
                new LocalCache<>(tempCacheDir.resolve("backfill_test.json").toString(), cacheParameter);
        Cache<ClassifierCacheKey> redisCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());

        // When: Only secondary (REST Redis) has a value
        String testKey = "backfill-key";
        String redisValue = "redis-only-value";
        redisCache.put(testKey, redisValue);

        assertNull(localCache.get(testKey, String.class));

        // Create hierarchical cache with NONE strategy (backfills primary from secondary)
        HierarchicalCache<ClassifierCacheKey> hierarchicalCache =
                new HierarchicalCache<>(cacheParameter, localCache, redisCache, CacheReplacementStrategy.NONE);

        // When: Get the value
        String result = hierarchicalCache.get(testKey, String.class);

        // Then: Value from secondary cache is returned
        assertEquals(redisValue, result);

        // And: Primary cache is backfilled with the value
        hierarchicalCache.flush();
        assertEquals(redisValue, localCache.get(testKey, String.class));
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForServerReady(String baseUrl) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            if (isServerResponding(baseUrl)) {
                return;
            }
            Thread.sleep(150);
        }
        throw new IllegalStateException("REST-Redis server did not become ready in time");
    }

    private static boolean isServerResponding(String baseUrl) {
        try {
            var response = Unirest.get(baseUrl + "/").asString();
            return response.getStatus() >= 200 && response.getStatus() < 600;
        } catch (Exception e) {
            return false;
        }
    }
}
