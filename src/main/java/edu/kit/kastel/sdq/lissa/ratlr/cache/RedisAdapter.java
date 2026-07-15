/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.util.Objects;

import redis.clients.jedis.UnifiedJedis;

/**
 * Adapter class that wraps a Jedis client to conform to the UnifiedRedisClient interface.
 */
/*package-private*/ class RedisAdapter implements UnifiedRedisClient {

    private final UnifiedJedis jedis;

    /**
     * Creates a new RedisAdapter instance with the given Jedis client.
     *
     * @param jedis The Jedis client to wrap
     */
    /*package-private*/ RedisAdapter(UnifiedJedis jedis) {
        this.jedis = Objects.requireNonNull(jedis);
    }

    /**
     * Pings the Redis server to check if it is available.
     *
     * @return true if the server responds, false otherwise.
     */
    @Override
    public boolean ping() {
        return jedis.ping().equals("PONG");
    }

    /**
     * Checks if a key exists in the Redis cache.
     *
     * @param key the key to check for existence
     * @return true if the key exists, false otherwise.
     */
    @Override
    public boolean exists(String key) {
        return jedis.exists(key);
    }

    /**
     * Retrieves the value of a field in a hash stored at key.
     *
     * @param key The key of the hash
     * @param field The field whose value is to be retrieved
     * @return Value for the field. If the key or field does not exist, null is returned.
     */
    @Override
    public String hget(String key, String field) {
        return jedis.hget(key, field);
    }

    /**
     * Sets the value of a field in a hash stored at a key
     *
     * @param key The key of the hash
     * @param field The field whose value is to be set
     * @param value The value to be set
     * @return The number of added fields
     */
    @Override
    public long hset(String key, String field, String value) {
        return jedis.hset(key, field, value);
    }

    /**
     * Shuts down the connection to the jedis instance.
     */
    @Override
    public void close() {
        jedis.close();
    }
}
