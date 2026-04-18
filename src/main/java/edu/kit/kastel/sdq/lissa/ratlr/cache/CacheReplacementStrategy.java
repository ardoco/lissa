/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

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
    NONE {
        @Override
        public <K extends CacheKey> String resolve(
                String key, String firstValue, Cache<K> firstCache, String secondValue, Cache<K> secondCache) {
            logger.info("Cache inconsistency detected for key {}, keeping both values (returning first value)", key);
            return firstValue;
        }
    },

    ERROR {
        @Override
        public <K extends CacheKey> String resolve(
                String key, String firstValue, Cache<K> firstCache, String secondValue, Cache<K> secondCache) {
            logger.error(
                    "Cache inconsistency detected for key {}, values: {} (first cache), {} (second cache)",
                    key,
                    firstValue,
                    secondValue);
            throw new IllegalStateException("Cache inconsistency detected for key " + key);
        }
    },

    OVERWRITE_FIRST {
        @Override
        public <K extends CacheKey> String resolve(
                String key, String firstValue, Cache<K> firstCache, String secondValue, Cache<K> secondCache) {
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
        @Override
        public <K extends CacheKey> String resolve(
                String key, String firstValue, Cache<K> firstCache, String secondValue, Cache<K> secondCache) {
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
     * Resolves a conflict between Redis and local cache values by applying the appropriate replacement strategy.
     *
     * @param <K> The type of cache key used in both caches
     * @param key The cache key where the conflict occurred
     * @param firstValue The value of the first cache
     * @param firstCache The first cache where the value was found
     * @param secondValue The value of the second cache
     * @param secondCache The second cache where the value was found
     *
     * @return The resolved cache value to be used
     */
    public abstract <K extends CacheKey> String resolve(
            String key, String firstValue, Cache<K> firstCache, String secondValue, Cache<K> secondCache);
}
