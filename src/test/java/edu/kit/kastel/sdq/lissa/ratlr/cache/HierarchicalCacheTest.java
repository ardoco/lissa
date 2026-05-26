/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Tests for HierarchicalCache layer synchronization behavior.
 * These tests verify that HierarchicalCache correctly synchronizes reads and writes across
 * multiple cache layers without requiring a real Redis or Docker instance.
 *
 * For tests of conflict resolution strategies, see {@link CacheReplacementStrategyTest}.
 */
@NullMarked
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class HierarchicalCacheTest {
    private static final String TEST_KEY = "test-key";

    @Mock
    private Cache<TestCacheKey> primaryCache;

    @Mock
    private Cache<TestCacheKey> secondaryCache;

    @Mock
    private CacheParameter<TestCacheKey> cacheParameter;

    private TestCacheKey cacheKeyInstance;

    @BeforeEach
    void setUp() {
        cacheKeyInstance = TestCacheKey.of(cacheParameter, "test");
        when(cacheParameter.createCacheKey(anyString())).thenReturn(cacheKeyInstance);
    }

    @Test
    @DisplayName("put() writes to both primary and secondary cache")
    void testPutObjectWritesToBothCaches() {
        HierarchicalCache<TestCacheKey> cache =
                new HierarchicalCache<>(cacheParameter, primaryCache, secondaryCache, CacheReplacementStrategy.NONE);

        TestObject testObj = new TestObject("test", 42);
        cache.put(TEST_KEY, testObj);

        verify(primaryCache).put(eq(TEST_KEY), same(testObj));
        verify(secondaryCache).put(eq(TEST_KEY), same(testObj));
    }

    @Test
    @DisplayName("containsKey() returns true if primary cache contains key")
    void testContainsKeyInPrimary() {
        HierarchicalCache<TestCacheKey> cache =
                new HierarchicalCache<>(cacheParameter, primaryCache, secondaryCache, CacheReplacementStrategy.NONE);

        when(primaryCache.containsKey(TEST_KEY)).thenReturn(true);
        when(secondaryCache.containsKey(TEST_KEY)).thenReturn(false);

        assertTrue(cache.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("containsKey() returns true if secondary cache contains key")
    void testContainsKeyInSecondary() {
        HierarchicalCache<TestCacheKey> cache =
                new HierarchicalCache<>(cacheParameter, primaryCache, secondaryCache, CacheReplacementStrategy.NONE);

        when(primaryCache.containsKey(TEST_KEY)).thenReturn(false);
        when(secondaryCache.containsKey(TEST_KEY)).thenReturn(true);

        assertTrue(cache.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("containsKey() returns false if neither cache contains key")
    void testContainsKeyInNeither() {
        HierarchicalCache<TestCacheKey> cache =
                new HierarchicalCache<>(cacheParameter, primaryCache, secondaryCache, CacheReplacementStrategy.NONE);

        when(primaryCache.containsKey(TEST_KEY)).thenReturn(false);
        when(secondaryCache.containsKey(TEST_KEY)).thenReturn(false);

        assertFalse(cache.containsKey(TEST_KEY));
    }

    @Test
    @DisplayName("flush() flushes both caches")
    void testFlushBothCaches() {
        HierarchicalCache<TestCacheKey> cache =
                new HierarchicalCache<>(cacheParameter, primaryCache, secondaryCache, CacheReplacementStrategy.NONE);

        cache.flush();

        verify(primaryCache).flush();
        verify(secondaryCache).flush();
    }

    // ==================== Helper Classes ====================

    static class TestObject {
        public String name;
        public int value;

        @SuppressWarnings("unused")
        TestObject() {
            // For Jackson deserialization
        }

        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    static class TestCacheKey implements CacheKey {
        private final String keyValue;

        private TestCacheKey(String content) {
            this.keyValue = KeyGenerator.generateKey(content);
        }

        static TestCacheKey of(CacheParameter<TestCacheKey> cacheParameter, String content) {
            // Access cacheParameter to satisfy architecture test requirements
            String parameters = cacheParameter.parameters();
            return new TestCacheKey(content + parameters);
        }

        @Override
        public String toJsonKey() {
            return keyValue;
        }

        @Override
        public String localKey() {
            return keyValue;
        }
    }
}
