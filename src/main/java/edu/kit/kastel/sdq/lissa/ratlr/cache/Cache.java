/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static edu.kit.kastel.sdq.lissa.ratlr.cache.LocalCache.LOCAL_CACHE_NAME;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interface for cache implementations in the LiSSA framework.
 * This interface defines the contract for caching mechanisms that store and retrieve
 * values associated with cache keys.
 *
 * @param <K> The type of cache key used in this cache
 */
public interface Cache<K extends CacheKey> {
    /**
     * Retrieves a value from the cache and deserializes it to the specified type.
     *
     * @param <T> The type to deserialize the cached value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     */
    <T> @Nullable T get(String key, Class<T> clazz);

    /**
     * Retrieves a value from the cache and deserializes it to the specified type.
     * <b>DO NOT USE UNLESS YOU KNOW WHAT YOU ARE DOING.</b>
     *
     * @param <T> The type to deserialize the cached value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     * @deprecated This method exposes internal cache key handling and should not be used in general code.
     */
    @Deprecated(forRemoval = false)
    <T> @Nullable T getViaInternalKey(K key, Class<T> clazz);

    /**
     * Stores a string value in the cache.
     *
     * @param key The cache key to store the value under
     * @param value The string value to store
     */
    void put(String key, String value);

    /**
     * Stores a string value in the cache.
     *
     * @param <T> The type of the value to store
     * @param key The cache key to store the value under
     * @param value The value to store
     * @deprecated This method exposes internal cache key handling and should not be used in general code.
     */
    @Deprecated(forRemoval = false)
    <T> void putViaInternalKey(K key, T value);

    /**
     * Stores an object value in the cache.
     * The object will be serialized before storage.
     *
     * @param <T> The type of the value to store
     * @param key The cache key to store the value under
     * @param value The object value to store
     */
    <T> void put(String key, T value);

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
    boolean containsKey(String key);

    /**
     * Gets the cache parameters used to configure this cache.
     *
     * @return The cache parameters
     */
    CacheParameter<K> getCacheParameter();

    /**
     * Converts a JSON string to an object of the specified type.
     * If the target type is String, the JSON string is returned as is.
     *
     * @param <T> The type to convert to
     * @param jsonData The JSON string to convert
     * @param clazz The class of the target type
     * @param mapper The ObjectMapper instance to use for deserialization
     * @return The converted object, or null if jsonData is null
     * @throws IllegalArgumentException If the JSON cannot be deserialized to the target type
     */
    @SuppressWarnings("unchecked")
    static <T> @Nullable T convert(@Nullable String jsonData, Class<T> clazz, ObjectMapper mapper) {
        if (jsonData == null) {
            return null;
        }
        if (clazz == String.class) {
            return (T) jsonData;
        }

        try {
            return mapper.readValue(jsonData, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not deserialize object", e);
        }
    }

    /**
     * Factory method to create a cache instance by type name.
     * Supported types:
     * <ul>
     *     <li>"local" - LocalCache for file-based storage</li>
     *     <li>"redis" - RedisCache for Redis-based storage</li>
     * </ul>
     *
     * @param <K> The type of cache key
     * @param type The cache type name (case-insensitive)
     * @param cacheDir The directory for local cache storage
     * @param parameters The cache parameters
     * @param mapper The ObjectMapper for JSON operations
     * @return A cache instance of the specified type
     * @throws IllegalArgumentException If the type is not recognized or the cache cannot be created
     */
    static <K extends CacheKey> Cache<K> createByType(
            String type, CacheParameter<K> parameters, @Nullable String cacheDir, @Nullable ObjectMapper mapper) {
        return switch (type) {
            case LOCAL_CACHE_NAME -> {
                if (cacheDir == null) {
                    throw new IllegalArgumentException("Cache directory must be provided for local cache");
                }
                yield new LocalCache<>(cacheDir, parameters);
            }
            case "redis" -> {
                if (mapper == null) {
                    throw new IllegalArgumentException("ObjectMapper must be provided for Redis cache");
                }
                yield new RedisCache<>(parameters, mapper);
            }
            default ->
                throw new IllegalArgumentException("Unknown cache type: " + type + ". Supported types: local, redis");
        };
    }
}
