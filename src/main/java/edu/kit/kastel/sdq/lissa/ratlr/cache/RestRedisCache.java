/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.fuchss.restredis.client.Client;
import org.fuchss.restredis.client.ClientConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

/**
 * Implements a Redis-based cache for storing and retrieving values using a REST interface.
 *
 * @param <K> The type of cache key used in this cache
 */
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

    /**
     * Initiates the REST Redis connection using environment variables for configuration. The following environment variables are used:
     * <ul>
     *     <li>{@code REST_REDIS_URI}: The URI of the REST Redis server (default: {@code http://localhost:8080})</li>
     *     <li>{@code REST_REDIS_USERNAME}: The username for authentication (optional)</li>
     *     <li>{@code REST_REDIS_PASSWORD}: The password for authentication (optional)</li>
     * </ul>
     */
    private static UnifiedRedisClient createRedisConnection() {
        String restRedisUri = "http://localhost:8080";
        String restRedisUriEnv = Environment.getenv("REST_REDIS_URI");
        if (restRedisUriEnv != null) {
            restRedisUri = restRedisUriEnv;
        }
        String restRedisUsername = Environment.getenv("REST_REDIS_USERNAME");
        String restRedisPassword = Environment.getenv("REST_REDIS_PASSWORD");

        ClientConfiguration config = new ClientConfiguration(restRedisUri, restRedisUsername, restRedisPassword);
        UnifiedRedisClient redis = new RestRedisAdapter(new Client(config));

        // Check if connection is working
        if (!redis.ping()) {
            throw new IllegalStateException("Could not connect to redis");
        }

        return redis;
    }
}
