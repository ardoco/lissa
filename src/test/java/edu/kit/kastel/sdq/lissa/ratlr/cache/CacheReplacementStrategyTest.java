/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Comprehensive tests for CacheReplacementStrategy enum implementations.
 * These tests verify correct conflict resolution behavior with various data types
 * including strings, objects, and null values.
 */
@NullMarked
class CacheReplacementStrategyTest {
    private static final String TEST_KEY = "test-key";
    private static final String TEST_VALUE = "test-value";
    private static final TestObject TEST_OBJECT = new TestObject("test", 42);
    private static final String CONFLICTING_VALUE = "conflicting-value";

    @TempDir
    private Path tempCacheDir;

    private Cache<TestCacheKey> primaryCache;
    private Cache<TestCacheKey> secondaryCache;
    private TestCacheKey cacheKeyInstance;

    @BeforeEach
    void setUp() {
        primaryCache = createLocalCache("primary");
        secondaryCache = createLocalCache("secondary");
        cacheKeyInstance = TestCacheKey.of(new TestCacheParameter(), "test");
    }

    /**
     * Factory method to create a LocalCache instance for testing
     */
    private Cache<TestCacheKey> createLocalCache(String cachePrefix) {
        return new LocalCache<>(
                tempCacheDir.resolve(cachePrefix + "_cache.json").toString(), new TestCacheParameter());
    }

    // ==================== NONE Strategy String Tests ====================

    @Test
    @DisplayName("NONE strategy: with string values - identical")
    void testNoneStrategyStringIdentical() {
        // Given both caches have identical values
        primaryCache.put(TEST_KEY, TEST_VALUE);
        secondaryCache.put(TEST_KEY, TEST_VALUE);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(TEST_VALUE, secondaryCache.get(TEST_KEY, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.NONE;
        String primaryValue = primaryCache.get(TEST_KEY, String.class);
        String secondaryValue = secondaryCache.get(TEST_KEY, String.class);

        // When resolving the values
        String result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then the primary value is returned and caches are unchanged
        assertEquals(TEST_VALUE, result);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(TEST_VALUE, secondaryCache.get(TEST_KEY, String.class));
    }

    @Test
    @DisplayName("NONE strategy: with string values - conflicting")
    void testNoneStrategyStringConflicting() {
        // Given primary and secondary have different values
        primaryCache.put(TEST_KEY, TEST_VALUE);
        secondaryCache.put(TEST_KEY, CONFLICTING_VALUE);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(CONFLICTING_VALUE, secondaryCache.get(TEST_KEY, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.NONE;
        String primaryValue = primaryCache.get(TEST_KEY, String.class);
        String secondaryValue = secondaryCache.get(TEST_KEY, String.class);

        // When resolving the conflict
        String result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then the primary value is returned and caches remain unchanged
        assertEquals(TEST_VALUE, result);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(CONFLICTING_VALUE, secondaryCache.get(TEST_KEY, String.class));
    }

    @Test
    @DisplayName("NONE strategy: with null primary")
    void testNoneStrategyNullPrimary() {
        // Given primary is null but secondary has a value
        secondaryCache.put(TEST_KEY, TEST_VALUE);
        assertNull(primaryCache.get(TEST_KEY, String.class));
        assertEquals(TEST_VALUE, secondaryCache.get(TEST_KEY, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.NONE;
        String primaryValue = primaryCache.get(TEST_KEY, String.class);
        String secondaryValue = secondaryCache.get(TEST_KEY, String.class);

        // When resolving with null primary
        String result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then the secondary value is backfilled to primary and returned
        assertEquals(TEST_VALUE, result);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(TEST_VALUE, secondaryCache.get(TEST_KEY, String.class));
    }

    @Test
    @DisplayName("NONE strategy: with object values - deep equal but different instances")
    void testNoneStrategyObjectDeepEqual() {
        // Given both caches have objects with same content but different instances
        TestObject obj1 = new TestObject("test", 42);
        TestObject obj2 = new TestObject("test", 42);

        primaryCache.put(TEST_KEY, obj1);
        secondaryCache.put(TEST_KEY, obj2);
        assertEquals(obj1, primaryCache.get(TEST_KEY, TestObject.class));
        assertEquals(obj2, secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.NONE;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then primary value is returned (deep equal objects are considered same)
        assertEquals(obj1, result);
        assertEquals(obj2, result);
    }

    // ==================== ERROR Strategy Conflict Tests ====================

    @Test
    @DisplayName("ERROR strategy: throws when string values conflict")
    void testErrorStrategyStringConflict() {
        // Given primary and secondary have conflicting string values
        primaryCache.put(TEST_KEY, TEST_VALUE);
        secondaryCache.put(TEST_KEY, CONFLICTING_VALUE);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertEquals(CONFLICTING_VALUE, secondaryCache.get(TEST_KEY, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.ERROR;
        String primaryValue = primaryCache.get(TEST_KEY, String.class);
        String secondaryValue = secondaryCache.get(TEST_KEY, String.class);

        // When resolving conflicting values
        // Then an exception is thrown
        assertThrows(
                IllegalStateException.class,
                () -> strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache));
    }

    @Test
    @DisplayName("ERROR strategy: tolerates null vs non-null in different layers")
    void testErrorStrategyNullTolerance() {
        // Given primary has a value but secondary is null
        primaryCache.put(TEST_KEY, TEST_VALUE);
        assertEquals(TEST_VALUE, primaryCache.get(TEST_KEY, String.class));
        assertNull(secondaryCache.get(TEST_KEY, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.ERROR;
        String primaryValue = primaryCache.get(TEST_KEY, String.class);
        String secondaryValue = secondaryCache.get(TEST_KEY, String.class);

        // When resolving with one null value
        String result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then no exception is thrown and primary value is returned
        assertEquals(TEST_VALUE, result);
    }

    @Test
    @DisplayName("ERROR strategy: accepts identical objects")
    void testErrorStrategyIdenticalObjects() {
        // Given both caches have equal objects
        TestObject obj1 = new TestObject("test", 42);
        TestObject obj2 = new TestObject("test", 42);

        primaryCache.put(TEST_KEY, obj1);
        secondaryCache.put(TEST_KEY, obj2);
        assertEquals(obj1, primaryCache.get(TEST_KEY, TestObject.class));
        assertEquals(obj2, secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.ERROR;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving identical (deep equal) objects
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then no exception and primary value is returned
        assertEquals(obj1, result);
    }

    // ==================== OVERWRITE Strategy Tests ====================

    @Test
    @DisplayName("OVERWRITE strategy: overwrites secondary on conflict")
    void testOverwriteStrategyObjectConflict() {
        // Given secondary has a different value
        TestObject secondary = new TestObject("secondary", 2);

        primaryCache.put(TEST_KEY, TEST_OBJECT);
        secondaryCache.put(TEST_KEY, secondary);
        assertEquals(TEST_OBJECT, primaryCache.get(TEST_KEY, TestObject.class));
        assertEquals(secondary, secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving the conflict
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then primary value is used and secondary is overwritten
        assertEquals(TEST_OBJECT, result);
        assertEquals(TEST_OBJECT, secondaryCache.get(TEST_KEY, TestObject.class));
    }

    @Test
    @DisplayName("OVERWRITE strategy: does not overwrite when values are identical")
    void testOverwriteStrategyNoOverwriteOnIdentical() {
        // Given both caches have the same value
        primaryCache.put(TEST_KEY, TEST_OBJECT);
        secondaryCache.put(TEST_KEY, TEST_OBJECT);
        assertEquals(TEST_OBJECT, primaryCache.get(TEST_KEY, TestObject.class));
        assertEquals(TEST_OBJECT, secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving identical values
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then no overwrite occurs
        assertEquals(TEST_OBJECT, result);
    }

    @Test
    @DisplayName("OVERWRITE strategy: does backfill when secondary is null")
    void testOverwriteStrategyNoOverwriteWhenSecondaryNull() {
        // Given primary has value but secondary is empty
        primaryCache.put(TEST_KEY, TEST_OBJECT);
        assertEquals(TEST_OBJECT, primaryCache.get(TEST_KEY, TestObject.class));
        assertNull(secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving with null secondary
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then value is backfilled to secondary
        assertEquals(TEST_OBJECT, result);
        assertEquals(TEST_OBJECT, secondaryCache.get(TEST_KEY, TestObject.class));
    }

    @Test
    @DisplayName("OVERWRITE strategy: handles null primary with non-null secondary")
    void testOverwriteStrategyNullPrimary() {
        // Given secondary has value but primary is empty
        secondaryCache.put(TEST_KEY, TEST_OBJECT);
        assertNull(primaryCache.get(TEST_KEY, TestObject.class));
        assertEquals(TEST_OBJECT, secondaryCache.get(TEST_KEY, TestObject.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        TestObject primaryValue = primaryCache.get(TEST_KEY, TestObject.class);
        TestObject secondaryValue = secondaryCache.get(TEST_KEY, TestObject.class);

        // When resolving with null primary
        TestObject result = strategy.resolve(TEST_KEY, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then secondary value is backfilled to primary
        assertEquals(TEST_OBJECT, result);
        assertEquals(TEST_OBJECT, primaryCache.get(TEST_KEY, TestObject.class));
    }

    // ==================== ViaInternalKey Strategy Tests ====================

    @Test
    @DisplayName("NONE strategy via internal key: string backfill to primary when secondary has value")
    void testNoneStrategyViaInternalKeyStringBackfillPrimary() {
        // Given secondary has a string value but primary is null
        secondaryCache.putViaInternalKey(cacheKeyInstance, TEST_VALUE);
        assertNull(primaryCache.getViaInternalKey(cacheKeyInstance, String.class));
        assertEquals(TEST_VALUE, secondaryCache.getViaInternalKey(cacheKeyInstance, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.NONE;
        String primaryValue = primaryCache.getViaInternalKey(cacheKeyInstance, String.class);
        String secondaryValue = secondaryCache.getViaInternalKey(cacheKeyInstance, String.class);

        // When resolving via internal key
        String result = strategy.resolveViaInternalKey(
                cacheKeyInstance, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then the secondary value is backfilled to primary using internal key
        assertEquals(TEST_VALUE, result);
        // Verify it was stored under the internal key, not the string representation of the key
        assertEquals(TEST_VALUE, primaryCache.getViaInternalKey(cacheKeyInstance, String.class));
        assertEquals(TEST_VALUE, secondaryCache.getViaInternalKey(cacheKeyInstance, String.class));
    }

    @Test
    @DisplayName("OVERWRITE strategy via internal key: string overwrite of secondary cache")
    void testOverwriteStrategyViaInternalKeyStringConflict() {
        // Given both caches have different string values
        primaryCache.putViaInternalKey(cacheKeyInstance, TEST_VALUE);
        secondaryCache.putViaInternalKey(cacheKeyInstance, CONFLICTING_VALUE);
        assertEquals(TEST_VALUE, primaryCache.getViaInternalKey(cacheKeyInstance, String.class));
        assertEquals(CONFLICTING_VALUE, secondaryCache.getViaInternalKey(cacheKeyInstance, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        String primaryValue = primaryCache.getViaInternalKey(cacheKeyInstance, String.class);
        String secondaryValue = secondaryCache.getViaInternalKey(cacheKeyInstance, String.class);

        // When resolving via internal key
        String result = strategy.resolveViaInternalKey(
                cacheKeyInstance, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then primary value overwrites secondary via internal key
        assertEquals(TEST_VALUE, result);
        // Verify the secondary was updated with the internal key, not a converted string key
        assertEquals(TEST_VALUE, secondaryCache.getViaInternalKey(cacheKeyInstance, String.class));
    }

    @Test
    @DisplayName("OVERWRITE strategy via internal key: string backfill to secondary when primary is null")
    void testOverwriteStrategyViaInternalKeyStringBackfillSecondary() {
        // Given primary is null but secondary has a string value
        secondaryCache.putViaInternalKey(cacheKeyInstance, TEST_VALUE);
        assertNull(primaryCache.getViaInternalKey(cacheKeyInstance, String.class));
        assertEquals(TEST_VALUE, secondaryCache.getViaInternalKey(cacheKeyInstance, String.class));

        CacheReplacementStrategy strategy = CacheReplacementStrategy.OVERWRITE;
        String primaryValue = primaryCache.getViaInternalKey(cacheKeyInstance, String.class);
        String secondaryValue = secondaryCache.getViaInternalKey(cacheKeyInstance, String.class);

        // When resolving via internal key
        String result = strategy.resolveViaInternalKey(
                cacheKeyInstance, primaryValue, primaryCache, secondaryValue, secondaryCache);

        // Then the secondary value is backfilled to primary using internal key
        assertEquals(TEST_VALUE, result);
        assertEquals(TEST_VALUE, primaryCache.getViaInternalKey(cacheKeyInstance, String.class));
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestObject that)) return false;
            return value == that.value && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }

        @Override
        public String toString() {
            return "TestObject{" + "name='" + name + '\'' + ", value=" + value + '}';
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
        public String localKey() {
            return keyValue;
        }

        @Override
        public String toString() {
            return keyValue;
        }
    }

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
}
