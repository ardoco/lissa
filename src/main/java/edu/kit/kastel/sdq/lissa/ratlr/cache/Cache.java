/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.jspecify.annotations.Nullable;

/**
 * Interface for cache implementations in the LiSSA framework.
 * This interface defines the contract for caching mechanisms that store and retrieve
 * values associated with cache keys.
 */
public interface Cache {
    /**
     * Retrieves a value from the cache and deserializes it to the specified type.
     *
     * @param <T> The type to deserialize the cached value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     */
    <T> @Nullable T get(CacheKey key, Class<T> clazz);

    /**
     * Stores a string value in the cache.
     *
     * @param key The cache key to store the value under
     * @param value The string value to store
     */
    void put(CacheKey key, String value);

    /**
     * Stores an object value in the cache.
     * The object will be serialized before storage.
     *
     * @param <T> The type of the value to store
     * @param key The cache key to store the value under
     * @param value The object value to store
     */
    <T> void put(CacheKey key, T value);

    /**
     * Flushes any pending changes to the cache storage.
     * This method should be called to ensure all cached values are persisted.
     */
    void flush();

    /**
     * Returns true if this map contains a mapping for the specified key.
     * More formally, returns true if and only if this map contains a mapping for a key k such that Objects.equals(key, k).
     * (There can be at most one such mapping.)
     *
     * @param key The cache key to check for existence
     * @return true if this map contains a mapping for the specified key
     */
    boolean containsKey(CacheKey key);
}
