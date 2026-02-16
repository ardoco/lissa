/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;
import java.util.Objects;

import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

/**
 * Base interface for all configuration types in the LiSSA-RATLR framework.
 * This interface provides common functionality for configurations, including serialization
 * and identifier generation. Implementations include {@link EvaluationConfiguration} for
 * evaluation pipelines and {@link OptimizerConfiguration} for optimization pipelines.
 */
public interface Configuration {

    /**
     * Separator used in configuration names to split different parts of the name.
     * For example, "iterative_gpt" would be split into ["iterative", "gpt"].
     */
    String CONFIG_NAME_SEPARATOR = "_";

    /**
     * Serializes this configuration to JSON and finalizes all module configurations.
     * This method should be called before saving the configuration to ensure all
     * module configurations are properly finalized.
     *
     * @return A JSON string representation of this configuration
     * @throws UncheckedIOException If the configuration cannot be serialized
     */
    String serializeAndDestroyConfiguration() throws UncheckedIOException;

    /**
     * Generates a unique identifier for this configuration.
     * The identifier is created by combining the given prefix with a hash of
     * the configuration's string representation.
     *
     * @param prefix The prefix to use for the identifier
     * @return A unique identifier for this configuration
     * @throws NullPointerException If prefix is null
     */
    default String getConfigurationIdentifierForFile(String prefix) {
        return Objects.requireNonNull(prefix) + "_" + KeyGenerator.generateKey(this.toString());
    }
}
