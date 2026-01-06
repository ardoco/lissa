/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.time.Instant;
import java.util.*;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import redis.clients.jedis.UnifiedJedis;

/**
 * Implements a Redis-based cache with local file backup.
 * This class provides a caching mechanism that primarily uses Redis for storage,
 * with a local file cache as a fallback. It supports storing and retrieving both
 * string values and serialized objects.
 * <p>
 * The cache can operate in three modes:
 * 1. Redis-only: When Redis is available and local cache is not configured
 * 2. Local-only: When Redis is unavailable and local cache is configured
 * 3. Hybrid: When both Redis and local cache are available (default)
 */
class RedisCache implements Cache {
    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);
    private final ObjectMapper mapper;

    /**
     * Local file-based cache used as a backup.
     */
    private final @Nullable LocalCache localCache;

    /**
     * Redis client instance.
     */
    private @Nullable UnifiedJedis jedis;

    /**
     * Flag indicating whether to replace local cache entries on conflict with Redis values.
     */
    private final boolean replaceLocalCacheOnConflict;

    /**
     * Creates a new Redis cache instance with an optional local cache backup.
     *
     * @param localCache The local cache to use as backup, or null if no backup is needed
     * @throws IllegalArgumentException If neither Redis nor local cache can be initialized
     */
    RedisCache(@Nullable LocalCache localCache, boolean replaceLocalCacheOnConflict) {
        this.localCache = localCache == null || !localCache.isReady() ? null : localCache;
        mapper = new ObjectMapper();
        createRedisConnection();
        if (jedis == null && this.localCache == null) {
            throw new IllegalArgumentException("Could not create cache");
        }
        this.replaceLocalCacheOnConflict = replaceLocalCacheOnConflict;
    }

    @Override
    public void flush() {
        if (localCache != null) {
            localCache.write();
        }
    }

    @Override
    public boolean containsKey(CacheKey key) {
        if (jedis != null && jedis.exists(key.toJsonKey())) {
            return true;
        }
        return localCache != null && localCache.containsKey(key);
    }

    /**
     * Establishes a connection to the Redis server.
     * The Redis URL can be configured through the REDIS_URL environment variable.
     * If the connection fails, the cache will fall back to using only the local cache.
     */
    private void createRedisConnection() {
        try {
            String redisUrl = "redis://localhost:6379";
            if (Environment.getenv("REDIS_URL") != null) {
                redisUrl = Environment.getenv("REDIS_URL");
            }
            jedis = new UnifiedJedis(redisUrl);
            // Check if connection is working
            jedis.ping();
        } catch (Exception e) {
            logger.warn("Could not connect to Redis, using file cache instead");
            jedis = null;
        }
    }

    /**
     * Retrieves a value from the cache and deserializes it to the specified type.
     * The method first attempts to retrieve the value from Redis, and if not found,
     * falls back to the local cache.
     * If the value is found in the local cache and Redis is available, it will be synchronized to Redis.
     * If the value is found in Redis and the local cache is available, it will be synchronized to the local cache.
     * In case of a mismatch between Redis and local cache values, a warning is logged and the replacement strategy
     * is applied: if {@link #replaceLocalCacheOnConflict} is true, the Redis value takes precedence and replaces
     * the local cache value; otherwise, the Redis cache value is returned without modification.
     *
     * @param <T> The type to deserialize the value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     */
    @Override
    public synchronized <T> T get(CacheKey key, Class<T> clazz) {
        String jsonData = jedis == null ? null : jedis.hget(key.toJsonKey(), "data");
        if (localCache == null) {
            return convert(jsonData, clazz);
        }
        String localData = localCache.get(key);
        // Value is in redis cache but not in local cache
        if (localData == null && jsonData != null) {
            localCache.put(key, jsonData);
        }
        // Value is in local cache but not in redis cache
        if (localData != null && jsonData == null && jedis != null) {
            jedis.hset(key.toJsonKey(), "data", localData);
        }
        // Value is in both caches, but they differ
        if (replaceLocalCacheOnConflict && jsonData != null && localData != null && !jsonData.equals(localData)) {
            logger.info("Cache inconsistency detected for key {}, using Redis value and replacing local one", key);
            localCache.put(key, jsonData);
        }

        String valueToReturn = jsonData != null ? jsonData : localData;
        return convert(valueToReturn, clazz);
    }

    /**
     * Converts a JSON string to an object of the specified type.
     * If the target type is String, the JSON string is returned as is.
     *
     * @param <T> The type to convert to
     * @param jsonData The JSON string to convert
     * @param clazz The class of the target type
     * @return The converted object, or null if jsonData is null
     * @throws IllegalArgumentException If the JSON cannot be deserialized to the target type
     */
    @SuppressWarnings("unchecked")
    private <T> @Nullable T convert(@Nullable String jsonData, Class<T> clazz) {
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
     * Stores a string value in the cache.
     * The value is stored in both Redis (if available) and the local cache (if configured).
     * When storing in Redis, a timestamp is also recorded.
     *
     * @param key The cache key to store the value under
     * @param value The string value to store
     */
    @Override
    public synchronized void put(CacheKey key, String value) {
        if (jedis != null) {
            String jsonKey = key.toJsonKey();
            jedis.hset(jsonKey, "data", value);
            jedis.hset(jsonKey, "timestamp", String.valueOf(Instant.now().getEpochSecond()));
        }
        if (localCache != null) {
            localCache.put(key, value);
        }
    }

    /**
     * Stores an object value in the cache.
     * The object is serialized to JSON before storage.
     *
     * @param <T> The type of the value to store
     * @param key The cache key to store the value under
     * @param value The object value to store
     * @throws IllegalArgumentException If the object cannot be serialized to JSON
     * @throws NullPointerException If value is null
     */
    @Override
    public synchronized <T> void put(CacheKey key, T value) {
        try {
            put(key, mapper.writeValueAsString(Objects.requireNonNull(value)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object", e);
        }
    }
}
