/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Represents a key for caching operations in the LiSSA framework.
 * This record is used to uniquely identify cached values based on various parameters
 * such as the model used, seed value, operation mode, and content.
 * <p>
 * The key can be serialized to JSON for storage and retrieval from the cache.
 * <p>
 * Please always use the {@link #of(String, int, double, Mode, String, Element, Element)} method to create a new instance.
 *
 * @param model The identifier of the model used for the cached operation.
 * @param seed The seed value used for randomization in the cached operation.
 * @param temperature The temperature setting used in the cached operation.
 * @param mode The mode of operation that was cached (e.g., embedding generation or chat
 * @param content The content that was processed in the cached operation.
 * @param localKey A local key for additional identification, not included in JSON serialization.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CacheKey(
        String model, int seed, double temperature, Mode mode, String content, @JsonIgnore String localKey) {
    /**
     * Defines the possible modes of operation that can be cached.
     */
    public enum Mode {
        /**
         * Mode for caching embedding generation operations.
         */
        EMBEDDING,

        /**
         * Mode for caching chat-based operations.
         */
        CHAT
    }

    /**
     * ObjectMapper instance configured for JSON serialization with indentation.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    public static CacheKey of(String model, int seed, double temperature, Mode mode, String content, Element source, Element target) {
        CacheKey cacheKey = new CacheKey(model, seed, temperature, mode, content, KeyGenerator.generateKey(content));
        CacheManager.getDefaultInstance().register(source, target, cacheKey);
        return cacheKey;
    }

    public static CacheKey of(String model, int seed, double temperature, Mode mode, String content) {
        return new CacheKey(model, seed, temperature, mode, content, KeyGenerator.generateKey(content));
    }

    /**
     * Only use this method if you want to use a custom local key. You mostly do not want to do this. Only for special handling of embeddings.
     * You should always prefer the {@link #of(String, int, double, Mode, String, Element, Element)} method.
     * @deprecated please use {@link #of(String, int, double, Mode, String, Element, Element)} instead.
     */
    @Deprecated(forRemoval = false)
    public static CacheKey ofRaw(
            String model, int seed, double temperature, Mode mode, String content, String localKey) {
        return new CacheKey(model, seed, temperature, mode, content, localKey);
    }

    /**
     * Converts this cache key to a JSON string representation.
     * The resulting string can be used as a unique identifier for the cached value.
     *
     * @return A JSON string representation of this cache key
     * @throws IllegalArgumentException If the key cannot be serialized to JSON
     */
    public String toJsonKey() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize key", e);
        }
    }
}
