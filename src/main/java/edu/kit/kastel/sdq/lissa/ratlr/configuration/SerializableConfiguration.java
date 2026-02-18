/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;

/**
 * Base interface for all storable configuration types in the LiSSA-RATLR framework.
 * This interface provides common functionality for configurations, including serialization
 * and identifier generation. Implementations include {@link EvaluationConfiguration} for
 * evaluation pipelines and {@link OptimizerConfiguration} for optimization pipelines.
 */
public interface SerializableConfiguration extends Configuration {

    /**
     * Serializes this configuration to JSON and finalizes all module configurations.
     * This method should be called before saving the configuration to ensure all
     * module configurations are properly finalized.
     *
     * @return A JSON string representation of this configuration
     * @throws UncheckedIOException If the configuration cannot be serialized
     */
    String serializeAndDestroyConfiguration() throws UncheckedIOException;
}
