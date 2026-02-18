/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;

import io.soabase.recordbuilder.core.RecordBuilder;

/**
 * Represents the complete configuration for a trace link analysis run.
 * This record contains all necessary configurations for artifact providers,
 * preprocessors, embedding creators, stores, classifiers, and postprocessors.
 * It supports both single-classifier and multi-stage classifier configurations.
 * <p>
 * The configuration is used to instantiate pipeline components, each of which can access shared context
 * via a {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} passed to their factory methods.
 * </p>
 */
@RecordBuilder()
public record EvaluationConfiguration(
        /**
         * Directory for caching intermediate results.
         */
        @JsonProperty("cache_dir") String cacheDir,

        /**
         * EvaluationConfiguration for gold standard evaluation.
         */
        @JsonProperty("gold_standard_configuration") GoldStandardConfiguration goldStandardConfiguration,

        /**
         * EvaluationConfiguration for the source artifact provider.
         */
        @JsonProperty("source_artifact_provider") ModuleConfiguration sourceArtifactProvider,

        /**
         * EvaluationConfiguration for the target artifact provider.
         */
        @JsonProperty("target_artifact_provider") ModuleConfiguration targetArtifactProvider,

        /**
         * EvaluationConfiguration for the source artifact preprocessor.
         */
        @JsonProperty("source_preprocessor") ModuleConfiguration sourcePreprocessor,

        /**
         * EvaluationConfiguration for the target artifact preprocessor.
         */
        @JsonProperty("target_preprocessor") ModuleConfiguration targetPreprocessor,

        /**
         * EvaluationConfiguration for the embedding creator.
         */
        @JsonProperty("embedding_creator") ModuleConfiguration embeddingCreator,

        /**
         * EvaluationConfiguration for the source element store.
         */
        @JsonProperty("source_store") ModuleConfiguration sourceStore,

        /**
         * EvaluationConfiguration for the target element store.
         */
        @JsonProperty("target_store") ModuleConfiguration targetStore,

        /**
         * EvaluationConfiguration for a single classifier.
         * Either this or {@link #classifiers} must be set, but not both.
         */
        @JsonProperty("classifier") @Nullable ModuleConfiguration classifier,

        /**
         * EvaluationConfiguration for a multi-stage classifier pipeline.
         * Either this or {@link #classifier} must be set, but not both.
         */
        @JsonProperty("classifiers") @Nullable List<List<ModuleConfiguration>> classifiers,

        /**
         * EvaluationConfiguration for the result aggregator.
         */
        @JsonProperty("result_aggregator") ModuleConfiguration resultAggregator,

        /**
         * EvaluationConfiguration for the trace link ID postprocessor.
         */
        @JsonProperty("tracelinkid_postprocessor") @Nullable ModuleConfiguration traceLinkIdPostprocessor)
        implements EvaluationConfigurationBuilder.With, SerializableConfiguration {

    /**
     * Serializes this configuration to JSON and finalizes all module configurations.
     * This method should be called before saving the configuration to ensure all
     * module configurations are properly finalized.
     *
     * @return A JSON string representation of this configuration
     * @throws UncheckedIOException If the configuration cannot be serialized
     */
    @Override
    public String serializeAndDestroyConfiguration() {
        sourceArtifactProvider.finalizeForSerialization();
        targetArtifactProvider.finalizeForSerialization();
        sourcePreprocessor.finalizeForSerialization();
        targetPreprocessor.finalizeForSerialization();
        embeddingCreator.finalizeForSerialization();
        sourceStore.finalizeForSerialization();
        targetStore.finalizeForSerialization();
        if (classifier != null) {
            classifier.finalizeForSerialization();
        }
        if (classifiers != null) {
            for (var group : classifiers) {
                for (var classifier : group) {
                    classifier.finalizeForSerialization();
                }
            }
        }
        resultAggregator.finalizeForSerialization();
        if (traceLinkIdPostprocessor != null) {
            traceLinkIdPostprocessor.finalizeForSerialization();
        }

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
        return "EvaluationConfiguration{" + "sourceArtifactProvider="
                + sourceArtifactProvider + ", targetArtifactProvider="
                + targetArtifactProvider + ", sourcePreprocessor="
                + sourcePreprocessor + ", targetPreprocessor="
                + targetPreprocessor + ", embeddingCreator="
                + embeddingCreator + ", sourceStore="
                + sourceStore + ", targetStore="
                + targetStore + ", classifier="
                + classifier + ", classifiers="
                + classifiers + ", resultAggregator="
                + resultAggregator + ", traceLinkIdPostprocessor="
                + traceLinkIdPostprocessor + '}';
    }

    /**
     * Creates a classifier instance based on this configuration.
     * Either a single classifier or a multi-stage classifier pipeline is created,
     * depending on which configuration is set. The shared {@link ContextStore} is passed to all classifiers.
     *
     * @param contextStore The shared context store for pipeline components
     * @return A classifier instance
     * @throws IllegalStateException If neither or both classifier configurations are set
     */
    public Classifier createClassifier(ContextStore contextStore) {
        if ((classifier == null) == (classifiers == null)) {
            throw new IllegalStateException("Either 'classifier' or 'classifiers' must be set, but not both.");
        }

        return classifier != null
                ? Classifier.createClassifier(classifier, contextStore)
                : Classifier.createMultiStageClassifier(classifiers, contextStore);
    }
}
