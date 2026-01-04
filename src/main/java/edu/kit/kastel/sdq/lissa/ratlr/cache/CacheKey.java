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
     * Returns a local key for in-memory cache identification and logging purposes.
     * <p>
     * This key is:
     * <ul>
     *     <li>Excluded from JSON serialization (annotated with {@link com.fasterxml.jackson.annotation.JsonIgnore @JsonIgnore})</li>
     *     <li>Used for human-readable logging and debugging</li>
     * </ul>
     * <p>
     * The local key is separate from the JSON key ({@link #toJsonKey()}) because it enables custom key generation
     * strategies for special cases
     *
     * @return A string representing the local key, typically a UUID derived from the cache key's content
     */
    String localKey();
}
