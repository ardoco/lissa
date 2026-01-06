/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Represents the complete configuration for a trace link analysis run.
 * This record contains all necessary configurations for artifact providers,
 * preprocessors, embedding creators, stores, classifiers, and postprocessors.
 * It supports both single-classifier and multi-stage classifier configurations.
 *
 * @param evaluationConfiguration Configuration for the evaluation setup.
 * @param promptOptimizer Configuration for the prompt optimizer.
 *                        This is used to optimize prompts for better classification results.
 * @param metric Configuration for the metric used in optimization to assign a score to a prompt for a set of examples
 * @param evaluator Configuration for the evaluator used in optimization
 */
@RecordBuilder()
public record OptimizerConfiguration(
        @JsonUnwrapped Configuration evaluationConfiguration,
        @JsonProperty("prompt_optimizer") ModuleConfiguration promptOptimizer,
        @JsonProperty("metric") ModuleConfiguration metric,
        @JsonProperty("evaluator") ModuleConfiguration evaluator)
        implements OptimizerConfigurationBuilder.With {

    /**
     * Serializes this configuration to JSON and finalizes all module configurations.
     * This method should be called before saving the configuration to ensure all
     * module configurations are properly finalized.
     *
     * @return A JSON string representation of this configuration
     * @throws UncheckedIOException If the configuration cannot be serialized
     */
    public String serializeAndDestroyConfiguration() throws UncheckedIOException {
        evaluationConfiguration.serializeAndDestroyConfiguration();
        promptOptimizer.finalizeForSerialization();
        metric.finalizeForSerialization();
        evaluator.finalizeForSerialization();

        try {
            return new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a string representation of this configuration.
     * The string includes all module configurations except the cache directory
     * and gold standard configuration.
     *
     * @return A string representation of this configuration
     */
    @Override
    @NotNull
    public String toString() {
        return "Configuration{" + "evaluationConfiguration="
                + evaluationConfiguration + ", metric="
                + metric + ", evaluator="
                + evaluator + ", promptOptimizer="
                + promptOptimizer + '}';
    }

    /**
     * Generates a unique identifier for this configuration.
     * The identifier is created by combining the given prefix with a hash of
     * the configuration's string representation.
     *
     * @param prefix The prefix to use for the identifier
     * @return A unique identifier for this configuration
     * @throws NullPointerException If prefix is null
     */
    public String getConfigurationIdentifierForFile(String prefix) {
        return Objects.requireNonNull(prefix) + "_" + KeyGenerator.generateKey(this.toString());
    }
}
