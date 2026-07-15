/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.util.Objects;

import org.fuchss.restredis.client.Client;

/**
 * Adapter that wraps a {@link Client REST Redis client} and exposes it through
 * the {@link UnifiedRedisClient} interface. All method calls are delegated directly to the underlying
 * {@link Client} instance.
 *
 * @see UnifiedRedisClient
 */
/*package-private*/ class RestRedisAdapter implements UnifiedRedisClient {

    /** The underlying REST-based Redis client to which all calls are delegated. */
    private final Client restRedisClient;

    /**
     * Constructs a new {@code RestRedisAdapter} wrapping the given client.
     *
     * @param restRedisClient the REST Redis client to delegate to. Must not be {@code null}
     */
    /*package-private*/ RestRedisAdapter(Client restRedisClient) {
        this.restRedisClient = Objects.requireNonNull(restRedisClient);
    }

    /**
     * Sends a {@code PING} command to the Redis server.
     *
     * @return {@code true} if the server responds successfully. {@code false} otherwise
     */
    @Override
    public boolean ping() {
        return restRedisClient.ping();
    }

    /**
     * Checks whether the given key exists in Redis.
     *
     * @param key the key to look up
     * @return {@code true} if the key exists. {@code false} otherwise
     */
    @Override
    public boolean exists(String key) {
        return restRedisClient.exists(key);
    }

    /**
     * Retrieves the value of a field within a Redis hash.
     *
     * @param key   the hash key.
     * @param field the field within the hash.
     * @return the field's value, or {@code null} if the key or field does not exist
     */
    @Override
    public String hget(String key, String field) {
        return restRedisClient.hget(key, field);
    }

    /**
     * Sets a field-value pair within a Redis hash.
     *
     * @param key   the hash key.
     * @param field the field to set within the hash.
     * @param value the value to store.
     * @return the number of fields that were added
     */
    @Override
    public long hset(String key, String field, String value) {
        return restRedisClient.hset(key, field, value);
    }

    /**
     * Closes the underlying REST Redis client and releases any held resources.
     *
     * <p>After this method returns the adapter must not be used further.
     */
    @Override
    public void close() {
        restRedisClient.close();
    }
}
