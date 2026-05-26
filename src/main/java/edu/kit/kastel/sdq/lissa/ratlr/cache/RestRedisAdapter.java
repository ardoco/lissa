/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import org.fuchss.restredis.client.Client;

public class RestRedisAdapter implements UnifiedRedisClient {

    private final Client restRedisClient;

    RestRedisAdapter(Client restRedisClient) {
        this.restRedisClient = restRedisClient;
    }

    @Override
    public boolean ping() {
        return restRedisClient.ping();
    }

    @Override
    public boolean exists(String key) {
        return restRedisClient.exists(key);
    }

    @Override
    public String hget(String key, String field) {
        return restRedisClient.hget(key, field);
    }

    @Override
    public long hset(String key, String field, String value) {
        return restRedisClient.hset(key, field, value);
    }

    @Override
    public void close() {
        restRedisClient.close();
    }
}
