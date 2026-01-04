/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

/**
 * Interface for cache parameter implementations that define how cache keys are created and configured.
 * Implementations specify the parameters that make a cache unique (e.g., model name, seed, temperature)
 * and provide factory methods for creating cache keys.
 *
 * @param <K> The type of cache key this parameter creates
 */
public interface CacheParameter<K extends CacheKey> {
    /**
     * Provides a unique string based on the actual cache parameters.
     * This string is used for the file name of LocalCache and must uniquely identify the cache configuration.
     *
     * @return A unique string based on the cache parameters
     */
    String parameters();

    /**
     * Creates a cache key based on the content and the cache parameters.
     * The created key combines the cache configuration with the content to be cached.
     *
     * @param content The content to create the cache key for
     * @return The created cache key
     */
    K createCacheKey(String content);
}
