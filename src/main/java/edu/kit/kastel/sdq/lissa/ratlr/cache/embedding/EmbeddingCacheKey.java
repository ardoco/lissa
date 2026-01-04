/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.embedding;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.LargeLanguageModelCacheMode;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Represents a key for embedding caching operations in the LiSSA framework.
 * This class is used to uniquely identify cached values based on various parameters
 * such as the model used, seed value, operation mode, and content.
 * <p>
 * The key can be serialized to JSON for storage and retrieval from the cache.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EmbeddingCacheKey implements CacheKey {
    private final String model;
    private final int seed;
    private final double temperature;
    private final LargeLanguageModelCacheMode mode;
    private final String content;

    @JsonIgnore
    private final String localKey;

    /**
     * Creates a new embedding cache key with the specified parameters.
     *
     * @param model The identifier of the model used for the cached operation
     * @param seed The seed value used for randomization in the cached operation (-1 for backward compatibility)
     * @param temperature The temperature setting used in the cached operation (-1 for backward compatibility)
     * @param mode The mode of operation that was cached (embedding generation for backward compatibility)
     * @param content The content that was processed in the cached operation
     * @param localKey A local key for additional identification, not included in JSON serialization
     */
    private EmbeddingCacheKey(
            String model,
            int seed,
            double temperature,
            LargeLanguageModelCacheMode mode,
            String content,
            String localKey) {
        this.model = model;
        this.seed = seed;
        this.temperature = temperature;
        this.mode = mode;
        this.content = content;
        this.localKey = localKey;
    }

    /**
     * Creates an embedding cache key from the given cache parameter and content.
     * This is the preferred way to create cache keys.
     *
     * @param cacheParameter The cache parameter containing model configuration
     * @param content The content to be cached
     * @return A new embedding cache key
     */
    static EmbeddingCacheKey of(EmbeddingCacheParameter cacheParameter, String content) {
        return new EmbeddingCacheKey(
                cacheParameter.modelName(),
                -1,
                -1,
                LargeLanguageModelCacheMode.EMBEDDING,
                content,
                KeyGenerator.generateKey(content));
    }

    /**
     * Creates an embedding cache key with a custom local key.
     * Only use this method if you want to use a custom local key. You mostly do not want to do this.
     * Only for special handling of embeddings. You should always prefer the {@link #of(EmbeddingCacheParameter, String)} method.
     *
     * @param model The identifier of the model
     * @param content The content to be cached
     * @param localKey The custom local key
     * @return A new embedding cache key with the specified local key
     * @deprecated Please use {@link #of(EmbeddingCacheParameter, String)} instead
     */
    @Deprecated(forRemoval = false)
    public static EmbeddingCacheKey ofRaw(String model, String content, String localKey) {
        return new EmbeddingCacheKey(model, -1, -1, LargeLanguageModelCacheMode.EMBEDDING, content, localKey);
    }

    /**
     * Gets the identifier of the model used for the cached operation.
     *
     * @return The model identifier
     */
    public String model() {
        return model;
    }

    /**
     * Gets the seed value used for randomization in the cached operation.
     *
     * @return The seed value (-1 for backward compatibility)
     */
    public int seed() {
        return seed;
    }

    /**
     * Gets the temperature setting used in the cached operation.
     *
     * @return The temperature value (-1 for backward compatibility)
     */
    public double temperature() {
        return temperature;
    }

    /**
     * Gets the mode of operation that was cached.
     *
     * @return The cache mode
     */
    public LargeLanguageModelCacheMode mode() {
        return mode;
    }

    /**
     * Gets the content that was processed in the cached operation.
     *
     * @return The content
     */
    public String content() {
        return content;
    }

    @Override
    @JsonIgnore
    public String localKey() {
        return localKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (EmbeddingCacheKey) obj;
        return Objects.equals(this.model, that.model)
                && this.seed == that.seed
                && Double.doubleToLongBits(this.temperature) == Double.doubleToLongBits(that.temperature)
                && Objects.equals(this.mode, that.mode)
                && Objects.equals(this.content, that.content)
                && Objects.equals(this.localKey, that.localKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, seed, temperature, mode, content, localKey);
    }

    @Override
    public String toString() {
        return "EmbeddingCacheKey[" + "model=" + model + ", " + "seed=" + seed + ", " + "temperature=" + temperature
                + ", " + "mode=" + mode + ", " + "content=" + content + ", " + "localKey=" + localKey + ']';
    }
}
