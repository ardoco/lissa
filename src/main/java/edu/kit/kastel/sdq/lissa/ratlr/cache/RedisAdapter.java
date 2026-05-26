/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import redis.clients.jedis.UnifiedJedis;

public class RedisAdapter implements UnifiedRedisClient {

    private final UnifiedJedis jedis;

    RedisAdapter(UnifiedJedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public boolean ping() {
        // TODO Find out what ping should return
        return jedis.ping() != null;
    }

    @Override
    public boolean exists(String key) {
        return jedis.exists(key);
    }

    @Override
    public String hget(String key, String field) {
        return jedis.hget(key, field);
    }

    @Override
    public long hset(String key, String field, String value) {
        return jedis.hset(key, field, value);
    }

    @Override
    public void close() {
        jedis.close();
    }
}
