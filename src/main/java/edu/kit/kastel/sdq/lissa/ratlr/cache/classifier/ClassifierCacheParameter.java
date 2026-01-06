/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter;

public record ClassifierCacheParameter(String modelName, int seed, double temperature) implements CacheParameter {
    @Override
    public String parameters() {
        if (temperature == 0.0) {
            return String.join("_", modelName, String.valueOf(seed));
        } else {
            return String.join("_", modelName, String.valueOf(seed), String.valueOf(temperature));
        }
    }
}
