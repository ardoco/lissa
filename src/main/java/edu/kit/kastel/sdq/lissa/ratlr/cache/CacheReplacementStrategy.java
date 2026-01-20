/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.UnifiedJedis;

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
        public String resolve(
                CacheKey key,
                String redisValue,
                String localValue,
                LocalCache localCache,
                @Nullable UnifiedJedis jedis) {
            logger.info("Cache inconsistency detected for key {}, keeping both values (returning Redis value)", key);
            return redisValue;
        }
    },

    /**
     * Replaces the local cache value with the Redis value when a conflict is detected.
     * This strategy gives precedence to the Redis cache as the source of truth.
     */
    REPLACE_LOCAL_VALUE {
        @Override
        public String resolve(
                CacheKey key,
                String redisValue,
                String localValue,
                LocalCache localCache,
                @Nullable UnifiedJedis jedis) {
            logger.info("Cache inconsistency detected for key {}, using Redis value and replacing local one", key);
            localCache.put(key, redisValue);
            return redisValue;
        }
    },

    /**
     * Replaces the Redis cache value with the local cache value when a conflict is detected.
     * This strategy gives precedence to the local cache as the source of truth.
     */
    REPLACE_REDIS_VALUE {
        @Override
        public String resolve(
                CacheKey key,
                String redisValue,
                String localValue,
                LocalCache localCache,
                @Nullable UnifiedJedis jedis) {
            logger.info("Cache inconsistency detected for key {}, using local value and replacing Redis one", key);
            if (jedis != null) {
                jedis.hset(key.toJsonKey(), "data", localValue);
            }
            return localValue;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(CacheReplacementStrategy.class);

    /**
     * Resolves a conflict between Redis and local cache values by applying the appropriate replacement strategy.
     *
     * @param key The cache key where the conflict occurred
     * @param redisValue The value from Redis
     * @param localValue The value from the local cache
     * @param localCache The local cache instance
     * @param jedis The Redis client instance (may be null)
     *
     * @return The resolved cache value to be used
     */
    public abstract String resolve(
            CacheKey key, String redisValue, String localValue, LocalCache localCache, @Nullable UnifiedJedis jedis);
}
