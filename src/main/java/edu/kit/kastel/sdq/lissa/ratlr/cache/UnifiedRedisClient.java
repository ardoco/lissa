/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

public interface UnifiedRedisClient {
    boolean ping();

    boolean exists(String key);

    String hget(String key, String field);

    long hset(String key, String field, String value);

    void close();
}
