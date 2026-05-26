/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void setup() {
        restCache = new RestRedisCache<>(cacheParameter, new ObjectMapper());
    }

    @Test
    @DisplayName("Test REST Redis client connection")
    void testRestRedisConnection() {
        Cache<ClassifierCacheKey> cache = Cache.createByType(
                CacheType.REST_REDIS, new ClassifierCacheParameter("test", 1, 0.0), null, new ObjectMapper());
    }

    @Test
    @DisplayName("Test REST Redis cache set and get")
    void testRestRedisCacheSetAndGet() {
        restCache.put("key", "value");
        String value = restCache.get("key", String.class);
        assertEquals("value", value);
        String nonExistingValue = restCache.get("ajhosadljhjyhxcjkhljysdhjk", String.class);
        assertNull(nonExistingValue);
    }

    @Test
    @DisplayName("Test if key exists")
    void testExistsMethod() {
        restCache.put("key", "value");
        String value = restCache.get("key", String.class);
        assertEquals("value", value);
        ClassifierCacheKey cacheKey = cacheParameter.createCacheKey("key");
        assertTrue(restCache.exists(cacheKey.toJsonKey()));
        assertFalse(restCache.exists("nonExistingKey"));
    }
}
