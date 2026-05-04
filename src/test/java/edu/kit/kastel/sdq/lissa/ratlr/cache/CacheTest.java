/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Unit tests for LocalCache implementation.
 * These tests ensure that the local cache correctly persists and retrieves cache entries
 * while maintaining backward compatibility with existing cache files.
 */
@NullMarked
class CacheTest {
    @TempDir
    private Path tempCacheDir;

    @BeforeEach
    void setup() throws IOException {
        // Reset the default cache manager singleton for each test
        CacheManager.setCacheDir(tempCacheDir.toString());
    }

    @AfterEach
    void teardown() {
        // Clean up the cache manager after each test
        CacheManager.resetDefaultInstance();
    }

    @Test
    @DisplayName("New cache entries are written to cache file")
    void testWriteNewEntry() throws IOException {
        Cache<TestCacheKey> cache = createLocalCache();

        cache.put("key1", "value1");
        cache.flush();

        Path cacheFile = tempCacheDir.resolve("test_cache.json");
        assertTrue(Files.exists(cacheFile));
        String content = Files.readString(cacheFile);
        assertTrue(content.contains("value1"));
    }

    @Test
    @DisplayName("Existing cache entries are retrieved from cache file")
    void testRetrieveExistingEntry() {
        Cache<TestCacheKey> cache1 = createLocalCache();
        cache1.put("key1", "value1");
        cache1.flush();

        Cache<TestCacheKey> cache2 = createLocalCache();
        String value = cache2.get("key1", String.class);

        assertEquals("value1", value);
    }

    @Test
    @DisplayName("Objects are serialized and deserialized correctly")
    void testObjectSerialization() {
        Cache<TestCacheKey> cache = createLocalCache();
        TestObject obj = new TestObject("test", 42);
        cache.put("key1", obj);
        cache.flush();

        Cache<TestCacheKey> cache2 = createLocalCache();
        TestObject retrieved = cache2.get("key1", TestObject.class);

        assertNotNull(retrieved);
        assertEquals("test", retrieved.name);
        assertEquals(42, retrieved.value);
    }

    @Test
    @DisplayName("Legacy cache files are backward compatible")
    void testBackwardCompatibility() throws IOException {
        Path sourceCacheFile = Path.of("src/test/resources/cache/test-local-cache-sample.json");
        Path cacheFile = tempCacheDir.resolve("test_cache.json");
        Files.copy(sourceCacheFile, cacheFile);

        Cache<TestCacheKey> cache = createLocalCache();
        String value1 = cache.get("test-key-1", String.class);
        String value2 = cache.get("test-key-2", String.class);
        String value3 = cache.get("test-key-3", String.class);

        assertEquals("test-value-1", value1);
        assertEquals("test-value-2", value2);
        assertEquals("test-value-3", value3);
    }

    // Helper classes and methods

    /**
     * Simple test object for serialization/deserialization testing
     */
    static class TestObject {
        public String name = "";
        public int value;

        TestObject() {
            // For Jackson deserialization
        }

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * Mock CacheKey implementation for testing
     */
    static class TestCacheKey implements CacheKey {
        private final String localKeyValue;

        private TestCacheKey(String content) {
            this.localKeyValue = KeyGenerator.generateKey(content);
        }

        static TestCacheKey of(CacheParameter<TestCacheKey> cacheParameter, String content) {
            return new TestCacheKey(content);
        }

        @Override
        public String localKey() {
            return localKeyValue;
        }
    }

    /**
     * Mock CacheParameter implementation for testing
     */
    static class TestCacheParameter implements CacheParameter<TestCacheKey> {
        @Override
        public String parameters() {
            return "test-cache";
        }

        @Override
        public TestCacheKey createCacheKey(String content) {
            return TestCacheKey.of(this, content);
        }
    }

    /**
     * Factory method to create a LocalCache instance for testing
     */
    private Cache<TestCacheKey> createLocalCache() {
        return new LocalCache<>(tempCacheDir.resolve("test_cache.json").toString(), new TestCacheParameter());
    }
}
