/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter;

/**
 * Cache parameters for classifier operations.
 * This record encapsulates the configuration parameters that define a unique classifier cache,
 * including the model name, random seed, and temperature setting.
 *
 * @param modelName The name of the language model used for classification
 * @param seed The random seed for reproducible results
 * @param temperature The temperature parameter for controlling randomness in model outputs
 */
public record ClassifierCacheParameter(String modelName, int seed, double temperature)
        implements CacheParameter<ClassifierCacheKey> {
    @Override
    public String parameters() {
        // For backward compatibility, omit temperature if it is 0.0
        if (temperature == 0.0) {
            return String.join("_", modelName, String.valueOf(seed));
        } else {
            return String.join("_", modelName, String.valueOf(seed), String.valueOf(temperature));
        }
    }

    @Override
    public ClassifierCacheKey createCacheKey(String content) {
        return ClassifierCacheKey.of(this, content);
    }
}
