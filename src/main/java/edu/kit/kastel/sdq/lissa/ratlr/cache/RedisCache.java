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
class RedisCache<K extends CacheKey> implements Cache<K> {
    private static final Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private final CacheParameter<K> cacheParameter;

    /**
     * Local file-based cache used as a backup.
     */
    private final @Nullable LocalCache<K> localCache;

    private final ObjectMapper mapper;

    /**
     * Redis client instance.
     */
    private @Nullable UnifiedJedis jedis;

    /**
     * Strategy for resolving conflicts between Redis and local cache values.
     */
    private final CacheReplacementStrategy conflictResolution;

    /**
     * Creates a new Redis cache instance with an optional local cache backup.
     *
     * @param localCache The local cache to use as backup, or null if no backup is needed
     * @param conflictResolution Strategy for resolving conflicts between Redis and local cache values
     * @throws IllegalArgumentException If neither Redis nor local cache can be initialized
     */
    RedisCache(
            CacheParameter<K> cacheParameter,
            @Nullable LocalCache<K> localCache,
            CacheReplacementStrategy conflictResolution) {
        this.cacheParameter = Objects.requireNonNull(cacheParameter);
        this.localCache = localCache == null || !localCache.isReady() ? null : localCache;
        if (this.localCache != null && !this.getCacheParameter().equals(this.localCache.getCacheParameter())) {
            throw new IllegalArgumentException("Cache parameter of local cache does not match the one of Redis cache");
        }

        mapper = new ObjectMapper();
        createRedisConnection();
        if (jedis == null && this.localCache == null) {
            throw new IllegalArgumentException("Could not create cache");
        }
        this.conflictResolution = conflictResolution;
    }

    @Override
    public void flush() {
        if (localCache != null) {
            localCache.write();
        }
    }

    @Override
    public boolean containsKey(String key) {
        K cacheKey = cacheParameter.createCacheKey(key);
        if (jedis != null && jedis.exists(cacheKey.toJsonKey())) {
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
     * In case of a mismatch between Redis and local cache values, a warning is logged and the configured
     * {@link #conflictResolution} strategy is applied.
     *
     * @param <T> The type to deserialize the value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     */
    @Override
    public synchronized <T> T get(String key, Class<T> clazz) {
        K cacheKey = cacheParameter.createCacheKey(key);
        String jsonData = jedis == null ? null : jedis.hget(cacheKey.toJsonKey(), "data");
        if (localCache == null) {
            return Cache.convert(jsonData, clazz, mapper);
        }
        String localData = localCache.get(key, String.class);
        // Value is in redis cache but not in local cache
        if (localData == null && jsonData != null) {
            localCache.put(key, jsonData);
        }
        // Value is in local cache but not in redis cache
        if (localData != null && jsonData == null && jedis != null) {
            jedis.hset(cacheKey.toJsonKey(), "data", localData);
        }
        String valueToReturn;
        if (jsonData != null && localData != null && !jsonData.equals(localData)) {
            // Value is in both caches, but they differ - apply conflict resolution strategy
            valueToReturn = conflictResolution.resolve(key, jsonData, localCache, localData, this);
        } else {
            valueToReturn = jsonData != null ? jsonData : localData;
        }
        return Cache.convert(valueToReturn, clazz, mapper);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized <T> @Nullable T getViaInternalKey(K cacheKey, Class<T> clazz) {
        String jsonData = jedis == null ? null : jedis.hget(cacheKey.toJsonKey(), "data");
        if (localCache == null) {
            return Cache.convert(jsonData, clazz, mapper);
        }
        String localData = localCache.getViaInternalKey(cacheKey, String.class);
        // Value is in redis cache but not in local cache
        if (localData == null && jsonData != null) {
            localCache.putViaInternalKey(cacheKey, jsonData);
        }
        // Value is in local cache but not in redis cache
        if (localData != null && jsonData == null && jedis != null) {
            jedis.hset(cacheKey.toJsonKey(), "data", localData);
        }
        String valueToReturn;
        // Value is in both caches, but they differ
        if (jsonData != null && localData != null && !jsonData.equals(localData)) {
            valueToReturn = conflictResolution.resolve(cacheKey.toJsonKey(), localData, localCache, jsonData, this);
        } else {
            valueToReturn = jsonData != null ? jsonData : localData;
        }
        return Cache.convert(valueToReturn, clazz, mapper);
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
    public synchronized void put(String key, String value) {
        K cacheKey = cacheParameter.createCacheKey(key);
        if (jedis != null) {
            String jsonKey = cacheKey.toJsonKey();
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
    public synchronized <T> void put(String key, T value) {
        try {
            put(key, mapper.writeValueAsString(Objects.requireNonNull(value)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object", e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized <T> void putViaInternalKey(K key, T value) {
        String data;
        try {
            data = mapper.writeValueAsString(Objects.requireNonNull(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object", e);
        }
        if (jedis != null) {
            String jsonKey = key.toJsonKey();
            jedis.hset(jsonKey, "data", data);
            jedis.hset(jsonKey, "timestamp", String.valueOf(Instant.now().getEpochSecond()));
        }
        if (localCache != null) {
            localCache.putViaInternalKey(key, data);
        }
    }

    @Override
    public CacheParameter<K> getCacheParameter() {
        return this.cacheParameter;
    }
}
