/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

/**
 * A unified abstraction over a Redis client, providing a minimal set of
 * operations used for caching within the application.
 *
 * <p>Instances should be closed after use to release any held resources.
 */
public interface UnifiedRedisClient extends AutoCloseable {

    /**
     * Sends a {@code PING} command to the Redis server.
     *
     * @return {@code true} if the server responds successfully, {@code false} otherwise.
     */
    boolean ping();

    /**
     * Checks whether the given key exists in Redis.
     *
     * @param key the key to look up.
     * @return {@code true} if the key exists, {@code false} otherwise.
     */
    boolean exists(String key);

    /**
     * Retrieves the value of a field within a Redis hash.
     *
     * @param key   the hash key.
     * @param field the field within the hash.
     * @return the field's value, or {@code null} if the key or field does not exist.
     */
    String hget(String key, String field);

    /**
     * Sets a field-value pair within a Redis hash.
     *
     * @param key   the hash key.
     * @param field the field to set within the hash.
     * @param value the value to store.
     * @return the number of fields that were added (not updated).
     */
    long hset(String key, String field, String value);

    /**
     * Closes the client and releases any held resources.
     *
     * <p>After this method returns the client must not be used further.
     */
    void close();
}
