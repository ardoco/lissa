/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.time.Instant;
import java.util.*;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import redis.clients.jedis.RedisClient;

/**
 * Implements a Redis-based cache for storing and retrieving values. For multi-layer caching with
 * synchronization and conflict resolution, use {@link HierarchicalCache}.
 * <p>
 * The cache will fail to initialize if Redis is unavailable.
 *
 * @param <K> The type of cache key used in this cache
 */
class RedisCache<K extends CacheKey> implements Cache<K> {

    private final CacheParameter<K> cacheParameter;
    private final ObjectMapper mapper;

    /**
     * Redis client instance.
     */
    private final UnifiedRedisClient redis;

    /**
     * Creates a new Redis cache instance.
     * This constructor will throw an exception if Redis is unavailable.
     *
     * @param cacheParameter The cache parameter configuration
     * @param mapper The ObjectMapper for JSON operations
     * @throws IllegalStateException If Redis connection cannot be established
     */
    RedisCache(CacheParameter<K> cacheParameter, ObjectMapper mapper) {
        this(cacheParameter, mapper, createRedisConnection());
    }

    /**
     * Creates a Redis Cache instance with a custom redis connection
     *
     * @param cacheParameter The cache parameter configuration
     * @param mapper The ObjectMapper for JSON operations
     * @param redis The connected redis instance
     */
    protected RedisCache(CacheParameter<K> cacheParameter, ObjectMapper mapper, UnifiedRedisClient redis) {
        this.cacheParameter = Objects.requireNonNull(cacheParameter);
        this.mapper = Objects.requireNonNull(mapper);
        this.redis = Objects.requireNonNull(redis);
    }

    @Override
    public void flush() {
        // Redis doesn't require manual flushing
    }

    @Override
    public boolean containsKey(String key) {
        K cacheKey = cacheParameter.createCacheKey(key);
        return redis.exists(cacheKey.toJsonKey());
    }

    /**
     * Establishes a connection to the Redis server.
     * The Redis URL can be configured through the REDIS_URL environment variable.
     *
     * @throws IllegalStateException if Redis connection could not be established
     */
    private static RedisAdapter createRedisConnection() {
        String redisUrl = "redis://localhost:6379";
        if (Environment.getenv("REDIS_URL") != null) {
            redisUrl = Environment.getenv("REDIS_URL");
        }
        RedisAdapter redis = new RedisAdapter(RedisClient.create(redisUrl));
        // Check if connection is working
        if (!redis.ping()) {
            redis.close();
            throw new IllegalStateException("Could not connect to Redis. Make sure the container is up and running.");
        }
        return redis;
    }

    /**
     * Retrieves a value from the cache and deserializes it to the specified type.
     *
     * @param <T> The type to deserialize the value to
     * @param key The cache key to look up
     * @param clazz The class of the type to deserialize to
     * @return The deserialized value, or null if not found
     */
    @Override
    public synchronized <T> @Nullable T get(String key, Class<T> clazz) {
        K cacheKey = cacheParameter.createCacheKey(key);
        String jsonData = redis.hget(cacheKey.toJsonKey(), "data");
        return Cache.convert(jsonData, clazz, mapper);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized <T> @Nullable T getViaInternalKey(K cacheKey, Class<T> clazz) {
        String jsonData = redis.hget(cacheKey.toJsonKey(), "data");
        return Cache.convert(jsonData, clazz, mapper);
    }

    /**
     * Stores a string value in the cache.
     * When storing in Redis, a timestamp is also recorded.
     *
     * @param key The cache key to store the value under
     * @param value The string value to store
     */
    @Override
    public synchronized void put(String key, String value) {
        K cacheKey = cacheParameter.createCacheKey(key);
        String jsonKey = cacheKey.toJsonKey();
        redis.hset(jsonKey, "data", value);
        redis.hset(jsonKey, "timestamp", String.valueOf(Instant.now().getEpochSecond()));
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
        String jsonKey = key.toJsonKey();
        redis.hset(jsonKey, "data", data);
        redis.hset(jsonKey, "timestamp", String.valueOf(Instant.now().getEpochSecond()));
    }

    @Override
    public CacheParameter<K> getCacheParameter() {
        return this.cacheParameter;
    }
}
