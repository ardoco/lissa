/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines strategies for handling cache value replacement when conflicts occur between Redis and local cache.
 * A conflict occurs when the same key exists in both caches but with different values.
 */
public enum CacheReplacementStrategy {
    /**
     * Does not replace conflicting values - leaves both cache values as they are.
     * The Redis value will be returned when reading.
     */
    NONE,

    ERROR {
        /**
         * Throws an exception when a conflict is detected between the two caches.
         */
        @Override
        public <K extends CacheKey, T> @Nullable T resolve(
                String key,
                @Nullable T firstValue,
                Cache<K> firstCache,
                @Nullable T secondValue,
                Cache<K> secondCache) {
            super.resolve(key, firstValue, firstCache, secondValue, secondCache);
            if (firstValue == secondValue) {
                return firstValue;
            }
            logger.error(
                    "Cache inconsistency detected for key {}, values: {} (first cache), {} (second cache)",
                    key,
                    firstValue,
                    secondValue);
            throw new IllegalStateException("Cache inconsistency detected for key " + key);
        }
    },

    OVERWRITE_FIRST {
        /**
         * Overwrites the first cache value with the second cache value in case of a conflict, and returns the second cache value.
         */
        @Override
        public <K extends CacheKey, T> @Nullable T resolve(
                String key,
                @Nullable T firstValue,
                Cache<K> firstCache,
                @Nullable T secondValue,
                Cache<K> secondCache) {
            super.resolve(key, firstValue, firstCache, secondValue, secondCache);
            if (firstValue == secondValue) {
                return firstValue;
            }
            logger.warn(
                    "Cache inconsistency detected for key {}, overwriting first cache value with second cache value: {} -> {}",
                    key,
                    firstValue,
                    secondValue);
            firstCache.put(key, secondValue);
            return secondValue;
        }
    },

    OVERWRITE_SECOND {
        /**
         * Overwrites the second cache value with the first cache value in case of a conflict, and returns the first cache value.
         */
        @Override
        public <K extends CacheKey, T> @Nullable T resolve(
                String key,
                @Nullable T firstValue,
                Cache<K> firstCache,
                @Nullable T secondValue,
                Cache<K> secondCache) {
            super.resolve(key, firstValue, firstCache, secondValue, secondCache);
            if (firstValue == secondValue) {
                return firstValue;
            }
            logger.warn(
                    "Cache inconsistency detected for key {}, overwriting second cache value with first cache value: {} -> {}",
                    key,
                    secondValue,
                    firstValue);
            secondCache.put(key, firstValue);
            return firstValue;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(CacheReplacementStrategy.class);

    /**
     * Resolves a conflict between two caches by applying the appropriate replacement strategy.
     * If a value is null in one cache but not the other, it will be copied to the cache where it is missing.
     * <p>
     * The default implementation does not perform any replacement and simply returns the first value.
     *
     * @param <K> The type of cache key used in both caches
     * @param <T> The type of the cache values
     * @param key The cache key where the conflict occurred
     * @param firstValue The value of the first cache
     * @param firstCache The first cache where the value was found
     * @param secondValue The value of the second cache
     * @param secondCache The second cache where the value was found
     *
     * @return The resolved cache value to be used (may be null)
     */
    public <K extends CacheKey, T> @Nullable T resolve(
            String key, @Nullable T firstValue, Cache<K> firstCache, @Nullable T secondValue, Cache<K> secondCache) {
        if (firstValue == null && secondValue != null) {
            firstCache.put(key, secondValue);
        }
        if (firstValue != null && secondValue == null) {
            secondCache.put(key, firstValue);
        }
        return firstValue;
    }
}
