/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.embedding;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.LargeLanguageModelCacheMode;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Represents a key for embedding caching operations in the LiSSA framework.
 * This record is used to uniquely identify cached values based on various parameters
 * such as the model used, seed value, operation mode, and content.
 * <p>
 * The key can be serialized to JSON for storage and retrieval from the cache.
 * <p>
 * Please always use the {@link #of(EmbeddingCacheParameter, String)} method to create a new instance.
 *
 * @param model The identifier of the model used for the cached operation.
 * @param seed The seed value used for randomization in the cached operation (-1 for backward compatibility).
 * @param temperature The temperature setting used in the cached operation (-1 for backward compatibility).
 * @param mode The mode of operation that was cached (embedding generation for backward compatibility).
 * @param content The content that was processed in the cached operation.
 * @param localKey A local key for additional identification, not included in JSON serialization.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmbeddingCacheKey(
        String model,
        int seed,
        double temperature,
        LargeLanguageModelCacheMode mode,
        String content,
        @JsonIgnore String localKey)
        implements CacheKey {

    public static EmbeddingCacheKey of(EmbeddingCacheParameter cacheParameter, String content) {
        return new EmbeddingCacheKey(
                cacheParameter.modelName(),
                -1,
                -1,
                LargeLanguageModelCacheMode.EMBEDDING,
                content,
                KeyGenerator.generateKey(content));
    }

    /**
     * Only use this method if you want to use a custom local key. You mostly do not want to do this. Only for special handling of embeddings.
     * You should always prefer the {@link #of(EmbeddingCacheParameter, String)} method.
     * @deprecated please use {@link #of(EmbeddingCacheParameter, String)} instead.
     */
    @Deprecated(forRemoval = false)
    public static EmbeddingCacheKey ofRaw(String model, String content, String localKey) {
        return new EmbeddingCacheKey(model, -1, -1, LargeLanguageModelCacheMode.EMBEDDING, content, localKey);
    }
}
