/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheParameter;

/**
 * Integration test for the REST Redis interface.
 * TODO: maybe testcontainer or something?
 */
public class RestRedisIntegrationTest {

    RestRedisCache<ClassifierCacheKey> restCache;
    private final ClassifierCacheParameter cacheParameter = new ClassifierCacheParameter("test", 1, 0.0);

    @TempDir
    private Path tempCacheDir;

    @BeforeEach
    public void setup() {
        restCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());
    }

    /**
     * Tests that a connection to the redis client can be established.
     */
    @Test
    @DisplayName("Test REST Redis client connection")
    void testRestRedisConnection() {
        Cache<ClassifierCacheKey> cache = Cache.createByType(
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
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(
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
}
