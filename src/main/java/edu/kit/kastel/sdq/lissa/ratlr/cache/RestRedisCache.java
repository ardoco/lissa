/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.fuchss.restredis.client.Client;
import org.fuchss.restredis.client.ClientConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

public class RestRedisCache<K extends CacheKey> extends RedisCache<K> {

    /**
     * Creates a new Rest Redis cache instance.
     * This constructor will throw an exception if Rest Redis is unavailable.
     *
     * @param cacheParameter The cache parameter configuration
     * @param mapper The ObjectMapper for JSON operations
     * @throws IllegalArgumentException If Redis connection cannot be established
     */
    RestRedisCache(CacheParameter<K> cacheParameter, ObjectMapper mapper) {
        super(cacheParameter, mapper, createRedisConnection());
    }

    private static UnifiedRedisClient createRedisConnection() {
        String restRedisUri = "localhost";
        if (Environment.getenv("REST_REDIS_URI") != null) {
            restRedisUri = Environment.getenv("REST_REDIS_URI");
        }
        String restRedisUsername = "admin";
        if (Environment.getenv("REST_REDIS_USERNAME") != null) {
            restRedisUsername = Environment.getenv("REST_REDIS_USERNAME");
        }
        String restRedisPassword = "dummy";
        if (Environment.getenv("REST_REDIS_PASSWORD") != null) {
            restRedisPassword = Environment.getenv("REST_REDIS_PASSWORD");
        }

        ClientConfiguration config = new ClientConfiguration(restRedisUri, restRedisUsername, restRedisPassword);
        UnifiedRedisClient redis = new RestRedisAdapter(new Client(config));

        // Check if connection is working
        if (!redis.ping()) {
            throw new IllegalStateException("Could not connect to redis");
        }

        return redis;
    }
}
