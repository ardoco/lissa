/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.classifier;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.LargeLanguageModelCacheMode;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Represents a key for classification caching operations in the LiSSA framework.
 * This record is used to uniquely identify cached values based on various parameters
 * such as the model used, seed value, operation mode, and content.
 * <p>
 * The key can be serialized to JSON for storage and retrieval from the cache.
 * <p>
 * Please always use the {@link #of(ClassifierCacheParameter, String)} method to create a new instance.
 *
 * @param model The identifier of the model used for the cached operation.
 * @param seed The seed value used for randomization in the cached operation.
 * @param temperature The temperature setting used in the cached operation.
 * @param mode The mode of operation that was cached (classification for backward compatibility).
 * @param content The content that was processed in the cached operation.
 * @param localKey A local key for additional identification, not included in JSON serialization.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassifierCacheKey(
        String model,
        int seed,
        double temperature,
        LargeLanguageModelCacheMode mode,
        String content,
        @JsonIgnore String localKey)
        implements CacheKey {

    public static ClassifierCacheKey of(ClassifierCacheParameter cacheParameter, String content) {
        return new ClassifierCacheKey(
                cacheParameter.modelName(),
                cacheParameter.seed(),
                cacheParameter.temperature(),
                LargeLanguageModelCacheMode.CHAT,
                content,
                KeyGenerator.generateKey(content));
    }
}
