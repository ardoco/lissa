/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

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
public record Configuration(
        /**
         * Directory for caching intermediate results.
         */
        @JsonProperty("cache_dir") String cacheDir,

        /**
         * Configuration for gold standard evaluation.
         */
        @JsonProperty("gold_standard_configuration") GoldStandardConfiguration goldStandardConfiguration,

        /**
         * Configuration for the source artifact provider.
         */
        @JsonProperty("source_artifact_provider") ModuleConfiguration sourceArtifactProvider,

        /**
         * Configuration for the target artifact provider.
         */
        @JsonProperty("target_artifact_provider") ModuleConfiguration targetArtifactProvider,

        /**
         * Configuration for the source artifact preprocessor.
         */
        @JsonProperty("source_preprocessor") ModuleConfiguration sourcePreprocessor,

        /**
         * Configuration for the target artifact preprocessor.
         */
        @JsonProperty("target_preprocessor") ModuleConfiguration targetPreprocessor,

        /**
         * Configuration for the embedding creator.
         */
        @JsonProperty("embedding_creator") ModuleConfiguration embeddingCreator,

        /**
         * Configuration for the source element store.
         */
        @JsonProperty("source_store") ModuleConfiguration sourceStore,

        /**
         * Configuration for the target element store.
         */
        @JsonProperty("target_store") ModuleConfiguration targetStore,

        /**
         * Configuration for a single classifier.
         * Either this or {@link #classifiers} must be set, but not both.
         */
        @JsonProperty("classifier") @Nullable ModuleConfiguration classifier,

        /**
         * Configuration for a multi-stage classifier pipeline.
         * Either this or {@link #classifier} must be set, but not both.
         */
        @JsonProperty("classifiers") @Nullable List<List<ModuleConfiguration>> classifiers,

        /**
         * Configuration for the result aggregator.
         */
        @JsonProperty("result_aggregator") ModuleConfiguration resultAggregator,

        /**
         * Configuration for the trace link ID postprocessor.
         */
        @JsonProperty("tracelinkid_postprocessor") @Nullable ModuleConfiguration traceLinkIdPostprocessor)
        implements ConfigurationBuilder.With {

    /**
     * Serializes this configuration to JSON and finalizes all module configurations.
     * This method should be called before saving the configuration to ensure all
     * module configurations are properly finalized.
     *
     * @return A JSON string representation of this configuration
     * @throws UncheckedIOException If the configuration cannot be serialized
     */
    public String serializeAndDestroyConfiguration() throws UncheckedIOException {
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
        return "Configuration{" + "sourceArtifactProvider="
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
