/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.embedding;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter;

/**
 * Cache parameters for embedding operations.
 * This record encapsulates the configuration parameters that define a unique embedding cache.
 * For embeddings, only the model name is required as embeddings are deterministic.
 *
 * @param modelName The name of the embedding model used for generating embeddings
 */
public record EmbeddingCacheParameter(String modelName) implements CacheParameter<EmbeddingCacheKey> {
    @Override
    public String parameters() {
        return modelName;
    }

    @Override
    public EmbeddingCacheKey createCacheKey(String content) {
        return EmbeddingCacheKey.of(this, content);
    }
}
