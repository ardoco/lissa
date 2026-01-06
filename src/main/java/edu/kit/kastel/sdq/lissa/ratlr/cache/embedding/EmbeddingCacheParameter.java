/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.embedding;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter;

public record EmbeddingCacheParameter(String modelName) implements CacheParameter {
    @Override
    public String parameters() {
        return modelName;
    }
}
