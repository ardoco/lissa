/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines strategies for handling cache value replacement when conflicts occur between cache layers.
 * A conflict occurs when the same key exists in both caches but with different values.
 */
public enum CacheReplacementStrategy {
    /**
     * Does not replace conflicting values - leaves both cache values as they are.
     * The primary value will be returned when reading.
     */
    NONE,

    /**
     * Does not replace conflicting values - leaves both cache values as they are.
     * If a conflict is detected an exception will be thrown.
     */
    ERROR {
        /**
         * Throws an exception when a conflict is detected between the two caches.
         *
         * @deprecated This method exposes internal cache key handling and should not be used in general code.
         */
        @Override
        public <K extends CacheKey, T> @Nullable T resolve(
                String key,
                @Nullable T primaryValue,
                Cache<K> primaryCache,
                @Nullable T secondaryValue,
                Cache<K> secondaryCache) {
            if (primaryValue != null && secondaryValue != null && !Objects.deepEquals(primaryValue, secondaryValue)) {
                logger.error(
                        "Cache inconsistency detected for key {}, values: {} (primary cache), {} (secondary cache)",
                        key,
                        primaryValue,
                        secondaryValue);
                throw new IllegalStateException("Cache inconsistency detected for key " + key);
            }
            return super.resolve(key, primaryValue, primaryCache, secondaryValue, secondaryCache);
        }

        /**
         * Throws an exception when a conflict is detected between the two caches.
         */
        @Override
        @Deprecated(forRemoval = false)
        <K extends CacheKey, T> @Nullable T resolveViaInternalKey(
                K key,
                @Nullable T primaryValue,
                Cache<K> primaryCache,
                @Nullable T secondaryValue,
                Cache<K> secondaryCache) {
            if (primaryValue != null && secondaryValue != null && !Objects.deepEquals(primaryValue, secondaryValue)) {
                logger.error(
                        "Cache inconsistency detected for key {}, values: {} (primary cache), {} (secondary cache)",
                        key,
                        primaryValue,
                        secondaryValue);
                throw new IllegalStateException("Cache inconsistency detected for key " + key);
            }
            return super.resolveViaInternalKey(key, primaryValue, primaryCache, secondaryValue, secondaryCache);
        }
    },

    /**
     * Replaces the conflicting value in the secondary cache with the value from the primary cache.
     */
    OVERWRITE {
        /**
         * Overwrites the secondary cache value with the primary cache value in case of a conflict, and returns the primary cache value.
         */
        @Override
        public <K extends CacheKey, T> @Nullable T resolve(
                String key,
                @Nullable T primaryValue,
                Cache<K> primaryCache,
                @Nullable T secondaryValue,
                Cache<K> secondaryCache) {
            if (primaryValue != null && secondaryValue != null && !Objects.deepEquals(primaryValue, secondaryValue)) {
                logger.warn(
                        "Cache inconsistency detected for key {}, overwriting secondary cache value with primary cache value: {} -> {}",
                        key,
                        secondaryValue,
                        primaryValue);
                secondaryCache.put(key, primaryValue);
                return primaryValue;
            }
            return super.resolve(key, primaryValue, primaryCache, secondaryValue, secondaryCache);
        }

        /**
         * Overwrites the secondary cache value with the primary cache value in case of a conflict, and returns the primary cache value.
         *
         * @deprecated This method exposes internal cache key handling and should not be used in general code.
         */
        @Override
        @Deprecated(forRemoval = false)
        <K extends CacheKey, T> @Nullable T resolveViaInternalKey(
                K key,
                @Nullable T primaryValue,
                Cache<K> primaryCache,
                @Nullable T secondaryValue,
                Cache<K> secondaryCache) {
            if (primaryValue != null && secondaryValue != null && !Objects.deepEquals(primaryValue, secondaryValue)) {
                logger.warn(
                        "Cache inconsistency detected for key {}, overwriting secondary cache value with primary cache value: {} -> {}",
                        key,
                        secondaryValue,
                        primaryValue);
                secondaryCache.putViaInternalKey(key, primaryValue);
                return primaryValue;
            }
            return super.resolveViaInternalKey(key, primaryValue, primaryCache, secondaryValue, secondaryCache);
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(CacheReplacementStrategy.class);

    /**
     * Resolves a conflict between two caches by applying the appropriate replacement strategy.
     * If a value is null in one cache but not the other, it will be copied to the cache where it is missing.
     * <p>
     * The default implementation does not perform any replacement and simply returns the primary value.
     *
     * @param <K> The type of cache key used in both caches
     * @param <T> The type of the cache values
     * @param key The cache key where the conflict occurred
     * @param primaryValue The value of the primary cache
     * @param primaryCache The primary cache where the value was found
     * @param secondaryValue The value of the secondary cache
     * @param secondaryCache The secondary cache where the value was found
     *
     * @return The resolved cache value to be used (may be null)
     */
    public <K extends CacheKey, T> @Nullable T resolve(
            String key,
            @Nullable T primaryValue,
            Cache<K> primaryCache,
            @Nullable T secondaryValue,
            Cache<K> secondaryCache) {
        if (primaryValue == null && secondaryValue != null) {
            primaryCache.put(key, secondaryValue);
            return secondaryValue;
        }
        if (primaryValue != null && secondaryValue == null) {
            secondaryCache.put(key, primaryValue);
            return primaryValue;
        }
        return primaryValue;
    }

    /**
     * Resolves a conflict between two caches by applying the appropriate replacement strategy.
     * If a value is null in one cache but not the other, it will be copied to the cache where it is missing.
     * <p>
     * The default implementation does not perform any replacement and simply returns the primary value.
     *
     * @param <K> The type of cache key used in both caches
     * @param <T> The type of the cache values
     * @param key The cache key where the conflict occurred
     * @param primaryValue The value of the primary cache
     * @param primaryCache The primary cache where the value was found
     * @param secondaryValue The value of the secondary cache
     * @param secondaryCache The secondary cache where the value was found
     *
     * @return The resolved cache value to be used (may be null)
     * @deprecated This method exposes internal cache key handling and should not be used in general code.
     */
    @Deprecated(forRemoval = false)
    <K extends CacheKey, T> @Nullable T resolveViaInternalKey(
            K key,
            @Nullable T primaryValue,
            Cache<K> primaryCache,
            @Nullable T secondaryValue,
            Cache<K> secondaryCache) {
        if (primaryValue == null && secondaryValue != null) {
            primaryCache.putViaInternalKey(key, secondaryValue);
            return secondaryValue;
        }
        if (primaryValue != null && secondaryValue == null) {
            secondaryCache.putViaInternalKey(key, primaryValue);
        }
        return primaryValue;
    }
}
