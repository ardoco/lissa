/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Represents a key for caching operations in the LiSSA framework.
 *
 */
public interface CacheKey {
    /**
     * Shared ObjectMapper instance for JSON serialization.
     */
    ObjectMapper MAPPER = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    /**
     * Converts this cache key to a JSON string representation.
     * The resulting string can be used as a unique identifier for the cached value.
     *
     * @return A JSON string representation of this cache key
     */
    default String toJsonKey() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize key", e);
        }
    }

    /**
     * Returns the key used to address this entry in file-based caches and in log output.
     * <p>
     * This key is:
     * <ul>
     *     <li>Excluded from JSON serialization (annotated with {@link com.fasterxml.jackson.annotation.JsonIgnore @JsonIgnore})</li>
     *     <li>The key under which {@code LocalCache} stores entries in its JSON file</li>
     * </ul>
     * <p>
     * The local key is separate from the JSON key ({@link #toJsonKey()}): Redis-backed caches address entries by
     * {@link #toJsonKey()}, while file-based caches use this key, which is derived from the cached content only.
     *
     * @return A string representing the local key, a deterministic UUID derived from the cache key's content
     */
    String localKey();
}
