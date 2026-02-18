/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
 */
@RecordBuilder()
public record OptimizerConfiguration(
        @JsonUnwrapped EvaluationConfiguration evaluationConfiguration,
        @JsonProperty("prompt_optimizer") ModuleConfiguration promptOptimizer,
        @JsonProperty("metric") ModuleConfiguration metric)
        implements OptimizerConfigurationBuilder.With, SerializableConfiguration {

    @Override
    public String serializeAndDestroyConfiguration() {
        evaluationConfiguration.serializeAndDestroyConfiguration();
        promptOptimizer.finalizeForSerialization();
        metric.finalizeForSerialization();

        try {
            return new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
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
    public String toString() {
        return "Configuration{" + "evaluationConfiguration="
                + evaluationConfiguration + ", metric="
                + metric + ", promptOptimizer="
                + promptOptimizer + '}';
    }
}
