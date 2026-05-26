/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.jspecify.annotations.Nullable;

/**
 * Implements a hierarchical cache that composes multiple cache implementations.
 * This class manages synchronization and conflict resolution between multiple cache layers
 * (e.g., Redis and local file cache), providing a unified view across the cache hierarchy.
 * <p>
 * The cache hierarchy operates as follows:
 * 1. Attempts to retrieve/store values in the primary cache
 * 2. Falls back to secondary cache if missing in the primary
 * 3. Automatically synchronizes values between layers when needed
 * 4. Applies conflict resolution strategy when values differ between layers
 *
 * @param <K> The type of cache key used in this cache
 */
class HierarchicalCache<K extends CacheKey> implements Cache<K> {

    private final CacheParameter<K> cacheParameter;

    /**
     * Primary cache in the hierarchy (typically Redis).
     */
    private final Cache<K> primaryCache;

    /**
     * Secondary cache in the hierarchy (typically local file cache).
     */
    private final Cache<K> secondaryCache;

    /**
     * Strategy for resolving conflicts between cache layers.
     */
    private final CacheReplacementStrategy conflictResolution;

    /**
     * Creates a new hierarchical cache instance.
     *
     * @param cacheParameter The cache parameter configuration
     * @param primaryCache The primary cache (e.g., Redis), or null if not available
     * @param secondaryCache The secondary cache (e.g., local file), or null if not available
     * @param conflictResolution Strategy for resolving conflicts between cache layers
     */
    HierarchicalCache(
            CacheParameter<K> cacheParameter,
            Cache<K> primaryCache,
            Cache<K> secondaryCache,
            CacheReplacementStrategy conflictResolution) {
        this.cacheParameter = cacheParameter;
        this.primaryCache = primaryCache;
        this.secondaryCache = secondaryCache;
        this.conflictResolution = conflictResolution;
    }

    @Override
    public synchronized <T> @Nullable T get(String key, Class<T> clazz) {
        T primaryValue = primaryCache.get(key, clazz);
        T secondaryValue = secondaryCache.get(key, clazz);
        return conflictResolution.resolve(key, primaryValue, primaryCache, secondaryValue, secondaryCache);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized <T> @Nullable T getViaInternalKey(K key, Class<T> clazz) {
        T primaryValue = primaryCache.getViaInternalKey(key, clazz);
        T secondaryValue = secondaryCache.getViaInternalKey(key, clazz);
        return conflictResolution.resolveViaInternalKey(
                key, primaryValue, primaryCache, secondaryValue, secondaryCache);
    }

    @Override
    public synchronized void put(String key, String value) {
        primaryCache.put(key, value);
        secondaryCache.put(key, value);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized <T> void putViaInternalKey(K key, T value) {
        primaryCache.putViaInternalKey(key, value);
        secondaryCache.putViaInternalKey(key, value);
    }

    @Override
    public synchronized <T> void put(String key, T value) {
        primaryCache.put(key, value);
        secondaryCache.put(key, value);
    }

    @Override
    public void flush() {
        primaryCache.flush();
        secondaryCache.flush();
    }

    @Override
    public boolean containsKey(String key) {
        if (primaryCache.containsKey(key)) {
            return true;
        }
        return secondaryCache.containsKey(key);
    }

    @Override
    public CacheParameter<K> getCacheParameter() {
        return cacheParameter;
    }
}
