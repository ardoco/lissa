/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
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
 * 
 * @param cacheDir Directory for caching intermediate results.
 * @param goldStandardConfiguration Configuration for gold standard evaluation.
 * @param sourceArtifactProvider Configuration for the source artifact provider.
 * @param targetArtifactProvider Configuration for the target artifact provider.
 * @param sourcePreprocessor Configuration for a single source artifact preprocessor.
 *                           Either this or {@link #sourcePreprocessors} must be set, but not both.
 * @param sourcePreprocessors Configuration for a multi-stage source artifact preprocessor pipeline.
 *                            Either this or {@link #sourcePreprocessor} must be set, but not both.
 * @param targetPreprocessor Configuration for a single target artifact preprocessor.
 *                           Either this or {@link #targetPreprocessors} must be set, but not both.
 * @param targetPreprocessors Configuration for a multi-stage target artifact preprocessor pipeline.
 *                            Either this or {@link #targetPreprocessor} must be set, but not both.
 * @param embeddingCreator Configuration for the embedding creator.
 * @param sourceStore Configuration for the source element store.
 * @param targetStore Configuration for the target element store.
 * @param classifier Configuration for a single classifier.
 *                   Either this or {@link #classifiers} must be set, but not both.
 * @param classifiers Configuration for a multi-stage classifier pipeline.
 *                    Either this or {@link #classifier} must be set, but not both.
 * @param resultAggregator Configuration for the result aggregator.
 * @param traceLinkIdPostprocessor Configuration for the trace link ID postprocessor.
 */
@RecordBuilder()
public record Configuration(
        @JsonProperty("cache_dir") String cacheDir,
        @JsonProperty("gold_standard_configuration") GoldStandardConfiguration goldStandardConfiguration,
        @JsonProperty("source_artifact_provider") ModuleConfiguration sourceArtifactProvider,
        @JsonProperty("target_artifact_provider") ModuleConfiguration targetArtifactProvider,
        @JsonProperty("source_preprocessor") ModuleConfiguration sourcePreprocessor,
        @JsonProperty("source_preprocessors") List<ModuleConfiguration> sourcePreprocessors,
        @JsonProperty("target_preprocessor") ModuleConfiguration targetPreprocessor,
        @JsonProperty("target_preprocessors") List<ModuleConfiguration> targetPreprocessors,
        @JsonProperty("embedding_creator") ModuleConfiguration embeddingCreator,
        @JsonProperty("source_store") ModuleConfiguration sourceStore,
        @JsonProperty("target_store") ModuleConfiguration targetStore,
        @JsonProperty("classifier") ModuleConfiguration classifier,
        @JsonProperty("classifiers") List<List<ModuleConfiguration>> classifiers,
        @JsonProperty("result_aggregator") ModuleConfiguration resultAggregator,
        @JsonProperty("tracelinkid_postprocessor") ModuleConfiguration traceLinkIdPostprocessor)
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
        if (sourcePreprocessor != null) {
            sourcePreprocessor.finalizeForSerialization();
        }
        if (sourcePreprocessors != null) {
            for (ModuleConfiguration configuration : sourcePreprocessors) {
                configuration.finalizeForSerialization();
            }
        }
        if (targetPreprocessor != null) {
            targetPreprocessor.finalizeForSerialization();
        }
        if (targetPreprocessors != null) {
            for (ModuleConfiguration configuration : targetPreprocessors) {
                configuration.finalizeForSerialization();
            }
        }
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
    public String toString() {
        return "Configuration{" + "sourceArtifactProvider="
                + sourceArtifactProvider + ", targetArtifactProvider="
                + targetArtifactProvider + ", sourcePreprocessor="
                + sourcePreprocessor + ", sourcePreprocessors="
                + sourcePreprocessors + ", targetPreprocessor="
                + targetPreprocessor + ", targetPreprocessors="
                + targetPreprocessors + ", embeddingCreator="
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

    /**
     * Creates a preprocessor instance based on this configuration.
     * Either a single classifier or a multi-stage classifier pipeline is created,
     * depending on which configuration is set. The shared {@link ContextStore} is passed to all classifiers.
     *
     * @param contextStore The shared context store for pipeline components
     * @return A classifier instance
     * @throws IllegalStateException If neither or both classifier configurations are set
     */
    public Preprocessor<Artifact> createSourcePreprocessor(ContextStore contextStore) {
        if ((sourcePreprocessor == null) == (sourcePreprocessors == null)) {
            throw new IllegalStateException("Either 'sourcePreprocessor' or 'sourcePreprocessors' must be set, but not both.");
        }

        return Preprocessor.createPreprocessors(sourcePreprocessor != null ? List.of(sourcePreprocessor) : sourcePreprocessors, contextStore);
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
    public Preprocessor<Artifact> createTargetPreprocessor(ContextStore contextStore) {
        if ((targetPreprocessor == null) == (targetPreprocessors == null)) {
            throw new IllegalStateException("Either 'targetPreprocessor' or 'targetPreprocessors' must be set, but not both.");
        }

        return Preprocessor.createPreprocessors(targetPreprocessor != null ? List.of(targetPreprocessor) : targetPreprocessors, contextStore);
    }
}
