/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

/**
 * Integration tests for cache recovery scenarios across different cache layers.
 * These tests verify that data can be fully recovered when transferring between
 * local files and remote cache systems like Redis.
 */
@NullMarked
@Testcontainers
class CacheRecoveryIntegrationTest {

    @Container
    static final GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:latest").withExposedPorts(6379).withReuse(true);

    @TempDir
    static Path tempDir;

    private static Path toRedisEnv;
    private static Path toLocalEnv;

    @BeforeAll
    static void setUpRedis() throws IOException {
        redisContainer.start();
        String redisUrl = "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);

        String baseEnv = """
            OLLAMA_EMBEDDING_HOST=http://localhost:11434
            OLLAMA_HOST=http://localhost:11434
            OPENAI_ORGANIZATION_ID=DUMMY
            OPENAI_API_KEY=DUMMY
            CACHE_REPLACEMENT_STRATEGY=ERROR
            REDIS_URL=%s
            """.formatted(redisUrl);

        toRedisEnv = tempDir.resolve(".env-to-redis");
        Files.writeString(toRedisEnv, baseEnv + "CACHE_HIERARCHY=LOCAL,REDIS\n");

        toLocalEnv = tempDir.resolve(".env-to-local");
        Files.writeString(toLocalEnv, baseEnv + "CACHE_HIERARCHY=REDIS,LOCAL\n");
    }

    @BeforeEach
    void setUp() throws IOException {
        deleteDir("src/test/resources/warc/temp/");
        try (var jedis = new redis.clients.jedis.Jedis(redisContainer.getHost(), redisContainer.getMappedPort(6379))) {
            jedis.flushAll();
            assertEquals(0, jedis.dbSize(), "Redis should be empty before test");
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        redisContainer.close();
        deleteDir("src/test/resources/warc/temp/");
    }

    // ==================== Cache Recovery Integration Tests ====================

    /**
     * Test the full cache recovery process from a local cache to a remote cache.
     * This test simulates a scenario where the local cache is initially used to backfill an empty Redis cache,
     * and then the local cache is recovered from Redis.
     * It verifies that the initial local caches contents are fully recovered to the new local cache.
     */
    @Test
    @DisplayName("Cache recovery: Local → Redis → Local file recovery")
    void testCacheRecoveryIntegration() throws Exception {
        // ===== PHASE 1: Redis backfill from legacy cache =====
        // Setup: Local (legacy) as primary, empty Redis as secondary
        runEvaluation("src/test/resources/warc/config.json", toRedisEnv);
        System.out.println("Legacy cache backfilled to Redis");
        // ===== PHASE 2: Local cache recovery from Redis =====
        // Setup: Redis as primary, new local cache as secondary
        runEvaluation("src/test/resources/warc/config-without-cache.json", toLocalEnv);
        System.out.println("Local cache recovered from Redis");

        assertCacheFilesIdentical(
                Path.of("src/test/resources/warc/cache/SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json"),
                Path.of("src/test/resources/warc/temp/cache/SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json"));
        assertCacheFilesIdentical(
                Path.of("src/test/resources/warc/cache/OpenAiEmbeddingCreator_text-embedding-3-large.json"),
                Path.of("src/test/resources/warc/temp/cache/OpenAiEmbeddingCreator_text-embedding-3-large.json"));
    }

    /**
     * This test fills the missing entries in a partial local cache from Redis.
     * This test simulates a scenario where the local cache already holds some values while others get backfilled
     * from the remote redis cache.
     */
    @Test
    @DisplayName("Cache completion: Partial local cache completed from Redis")
    void testPartialCacheCompletionIntegration() throws Exception {
        // ===== PHASE 1: Fill Redis from complete local cache =====
        runEvaluation("src/test/resources/warc/config.json", toRedisEnv);

        // ===== SETUP: Copy complete cache to temp dir and remove some entries =====
        Path sourceCacheFile =
                Path.of("src/test/resources/warc/cache/SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json");
        Path tempCacheDir = Path.of("src/test/resources/warc/temp/cache");
        Files.createDirectories(tempCacheDir);
        Path tempCacheFile = tempCacheDir.resolve("SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json");
        Files.copy(sourceCacheFile, tempCacheFile);

        // Remove some entries to simulate a partial cache
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
        Map<String, String> cacheData = new LinkedHashMap<>(mapper.readValue(tempCacheFile.toFile(), typeRef));
        List<String> keys = new ArrayList<>(cacheData.keySet());
        // Remove the last third of entries
        for (String key : keys.subList(keys.size() - keys.size() / 3, keys.size())) {
            cacheData.remove(key);
        }
        assertTrue(keys.size() > cacheData.size(), "Partial cache should not contain all cache keys");
        mapper.writeValue(tempCacheFile.toFile(), cacheData);

        // ===== PHASE 2: Complete partial local cache from Redis =====
        runEvaluation("src/test/resources/warc/config-without-cache.json", toLocalEnv);

        // ===== ASSERT: Partial local cache is now complete =====
        assertCacheFilesIdentical(sourceCacheFile, tempCacheFile);
    }

    /**
     * This test ensures that no cache values are removed when the local cache is already complete and the Redis cache
     * is used as secondary.
     */
    @Test
    @DisplayName("Cache preservation: Existing entries are not removed")
    void testCacheStabilityIntegration() throws Exception {
        // ===== SETUP: Copy complete cache to temp dir and add extra entries =====
        Path sourceCacheFile =
                Path.of("src/test/resources/warc/cache/SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json");
        Path tempCacheDir = Path.of("src/test/resources/warc/temp/cache");
        Files.createDirectories(tempCacheDir);
        Path tempCacheFile = tempCacheDir.resolve("SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json");
        Files.copy(sourceCacheFile, tempCacheFile);

        // Add extra entries to simulate a cache with additional values
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
        Map<String, String> cacheData = new LinkedHashMap<>(mapper.readValue(tempCacheFile.toFile(), typeRef));
        int keys = cacheData.size();
        cacheData.put("extra-key-1", "\"extra-value-1\"");
        cacheData.put("extra-key-2", "\"extra-value-2\"");
        assertTrue(keys < cacheData.size(), "Larger cache should contain additional keys");
        mapper.writeValue(tempCacheFile.toFile(), cacheData);

        // ===== PHASE 1: Fill Redis from complete local cache =====
        runEvaluation("src/test/resources/warc/config.json", toRedisEnv);

        // ===== PHASE 2: Run evaluation against fully populated local cache =====
        runEvaluation("src/test/resources/warc/config-without-cache.json", toLocalEnv);

        // ===== ASSERT: All original entries still present including extra ones =====
        Map<String, String> resultData = mapper.readValue(tempCacheFile.toFile(), typeRef);
        assertEquals(cacheData.size(), resultData.size(), "Cache should have same number of entries");
        for (String rawKey : cacheData.keySet()) {
            RecoveryCacheKey key = RecoveryCacheKey.of(new RecoveryCacheParameter(), rawKey);
            LocalCache<RecoveryCacheKey> cache1 =
                    new LocalCache<>(tempCacheFile.toString(), new RecoveryCacheParameter());
            String value1 = cache1.getViaInternalKey(key, String.class);
            assertNotNull(value1, "Value should not be null for key: " + rawKey);
        }
        // Extra keys should still be present
        LocalCache<RecoveryCacheKey> resultCache =
                new LocalCache<>(tempCacheFile.toString(), new RecoveryCacheParameter());
        assertEquals(
                "extra-value-1",
                resultCache.getViaInternalKey(
                        RecoveryCacheKey.of(new RecoveryCacheParameter(), "extra-key-1"), String.class));
        assertEquals(
                "extra-value-2",
                resultCache.getViaInternalKey(
                        RecoveryCacheKey.of(new RecoveryCacheParameter(), "extra-key-2"), String.class));
    }

    /**
     * Runs an actual evaluation with the given configuration and environment.
     *
     * @param configPath The path to the configuration file.
     * @param envPath The path to the environment file.
     * @throws IOException If any of the files cannot be read
     */
    private void runEvaluation(String configPath, Path envPath) throws IOException {
        Environment.overwrite(envPath);

        // Run evaluation that triggers cache reads
        File config = new File(configPath);
        Assertions.assertTrue(config.exists());

        Evaluation evaluation = new Evaluation(config.toPath());
        evaluation.run();
    }

    /**
     * Compares two cache files to ensure they have identical contents. Asserts that both files exist, have the same
     * number of entries, and that all key-value pairs match when retrieved from the local cache.
     * The cache files may mix serialized JSON and raw key-value pairs and be out of order from each other.
     */
    private void assertCacheFilesIdentical(Path cacheFile1, Path cacheFile2) throws Exception {
        assertTrue(Files.exists(cacheFile1), "First cache file should exist");
        assertTrue(Files.exists(cacheFile2), "Second cache file should exist");

        LocalCache<RecoveryCacheKey> cache1 = new LocalCache<>(cacheFile1.toString(), new RecoveryCacheParameter());
        LocalCache<RecoveryCacheKey> cache2 = new LocalCache<>(cacheFile2.toString(), new RecoveryCacheParameter());

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
        Map<String, String> rawKeys = mapper.readValue(cacheFile1.toFile(), typeRef);

        assertEquals(
                rawKeys.size(),
                mapper.readValue(cacheFile2.toFile(), typeRef).size(),
                "Cache files should have same number of entries");

        for (String rawKey : rawKeys.keySet()) {

            RecoveryCacheKey key = RecoveryCacheKey.of(new RecoveryCacheParameter(), rawKey);
            String value1 = cache1.getViaInternalKey(key, String.class);
            String value2 = cache2.getViaInternalKey(key, String.class);
            assertNotNull(value1, "Cached value should not be null for key: " + rawKey);
            assertEquals(value1, value2, "Values should match for key: " + rawKey);
        }
    }

    /**
     * Delete a directory recursively.
     */
    private static void deleteDir(String path) throws IOException {
        Path dir = Path.of(path);
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                List<File> files = stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .toList();
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Mock CacheKey implementation for testing. The local key is the raw key from the cache file.
     */
    private static class RecoveryCacheKey implements CacheKey {
        private final String localKeyValue;

        private RecoveryCacheKey(String content) {
            this.localKeyValue = content;
        }

        @SuppressWarnings("unused")
        static RecoveryCacheKey of(RecoveryCacheParameter cacheParameter, String content) {
            return new RecoveryCacheKey(content);
        }

        @Override
        public String localKey() {
            return localKeyValue;
        }
    }

    /**
     * Mock CacheParameter implementation for testing
     */
    private static class RecoveryCacheParameter implements CacheParameter<RecoveryCacheKey> {
        @Override
        public String parameters() {
            return "test-cache";
        }

        @Override
        public RecoveryCacheKey createCacheKey(String content) {
            return RecoveryCacheKey.of(this, content);
        }
    }
}
