/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.ArtifactProvider;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.ElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator.EmbeddingCreator;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;

/**
 * Represents a single evaluation run of the LiSSA framework.
 * This class manages the complete trace link analysis pipeline for a given configuration,
 * including:
 * <ul>
 *     <li>Artifact loading from source and target providers</li>
 *     <li>Preprocessing of artifacts into elements</li>
 *     <li>Embedding calculation for elements</li>
 *     <li>Classification of potential trace links</li>
 *     <li>Result aggregation and postprocessing</li>
 *     <li>Statistics generation and result storage</li>
 * </ul>
 * <p>
 * The pipeline uses a {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} to share context objects
 * between components such as artifact providers, preprocessors, embedding creators, classifiers, and aggregators.
 * </p>
 *
 * The pipeline follows these steps:
 * <ol>
 *     <li>Load artifacts from configured providers</li>
 *     <li>Preprocess artifacts into elements</li>
 *     <li>Calculate embeddings for elements</li>
 *     <li>Build element stores for efficient access</li>
 *     <li>Classify potential trace links</li>
 *     <li>Aggregate results into final trace links</li>
 *     <li>Postprocess trace link IDs</li>
 *     <li>Generate and save statistics</li>
 * </ol>
 */
public class Evaluation {

    private static final Logger logger = LoggerFactory.getLogger(Evaluation.class);
    private final Path configFile;

    private Configuration configuration;

    /** Provider for source artifacts */
    private ArtifactProvider sourceArtifactProvider;
    /** Provider for target artifacts */
    private ArtifactProvider targetArtifactProvider;
    /** Preprocessor for source artifacts */
    private Preprocessor sourcePreprocessor;
    /** Preprocessor for target artifacts */
    private Preprocessor targetPreprocessor;
    /** Creator for element embeddings */
    private EmbeddingCreator embeddingCreator;
    /** Store for source elements */
    private ElementStore sourceStore;
    /** Store for target elements */
    private ElementStore targetStore;
    /** Classifier for trace link analysis */
    private Classifier classifier;
    /** Aggregator for classification results */
    private ResultAggregator aggregator;
    /** Postprocessor for trace link IDs */
    private TraceLinkIdPostprocessor traceLinkIdPostProcessor;

    /**
     * Creates a new evaluation instance with the specified configuration file.
     * This constructor:
     * <ol>
     *     <li>Validates the configuration file path</li>
     *     <li>Loads and initializes the configuration</li>
     *     <li>Sets up all required components for the pipeline, sharing a {@link ContextStore}</li>
     * </ol>
     *
     * @param configFile Path to the configuration file
     * @throws IOException If there are issues reading the configuration file
     * @throws NullPointerException If configFile is null
     */
    public Evaluation(Path configFile) throws IOException {
        this.configFile = Objects.requireNonNull(configFile);
        setup();
    }

    /**
     * Sets up the evaluation pipeline components.
     * This method:
     * <ol>
     *     <li>Loads the configuration from file</li>
     *     <li>Initializes the cache manager</li>
     *     <li>Creates artifact providers</li>
     *     <li>Creates preprocessors</li>
     *     <li>Creates embedding creator</li>
     *     <li>Creates element stores</li>
     *     <li>Creates classifier</li>
     *     <li>Creates result aggregator</li>
     *     <li>Creates trace link ID postprocessor</li>
     * </ol>
     *
     * @throws IOException If there are issues reading the configuration
     */
    private void setup() throws IOException {
        configuration = new ObjectMapper().readValue(configFile.toFile(), Configuration.class);
        CacheManager.setCacheDir(configuration.cacheDir());

        ContextStore contextStore = new ContextStore();

        sourceArtifactProvider =
                ArtifactProvider.createArtifactProvider(configuration.sourceArtifactProvider(), contextStore);
        targetArtifactProvider =
                ArtifactProvider.createArtifactProvider(configuration.targetArtifactProvider(), contextStore);

        sourcePreprocessor = configuration.createSourcePreprocessor(contextStore);
        targetPreprocessor = configuration.createTargetPreprocessor(contextStore);

        embeddingCreator = EmbeddingCreator.createEmbeddingCreator(configuration.embeddingCreator(), contextStore);
        sourceStore = new ElementStore(configuration.sourceStore(), false);
        targetStore = new ElementStore(configuration.targetStore(), true);

        classifier = configuration.createClassifier(contextStore);
        aggregator = ResultAggregator.createResultAggregator(configuration.resultAggregator(), contextStore);

        traceLinkIdPostProcessor = TraceLinkIdPostprocessor.createTraceLinkIdPostprocessor(
                configuration.traceLinkIdPostprocessor(), contextStore);

        configuration.serializeAndDestroyConfiguration();
    }

    /**
     * Gets the configuration used for this evaluation.
     *
     * @return The configuration object
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Runs the complete trace link analysis pipeline.
     * This method:
     * <ol>
     *     <li>Loads artifacts from providers</li>
     *     <li>Preprocesses artifacts into elements</li>
     *     <li>Calculates embeddings for elements</li>
     *     <li>Builds element stores</li>
     *     <li>Classifies potential trace links</li>
     *     <li>Aggregates results</li>
     *     <li>Postprocesses trace link IDs</li>
     *     <li>Generates and saves statistics</li>
     * </ol>
     *
     * @return Set of identified trace links
     */
    public Set<TraceLink> run() {
        // RUN
        logger.info("Loading artifacts");
        var sourceArtifacts = sourceArtifactProvider.getArtifacts();
        var targetArtifacts = targetArtifactProvider.getArtifacts();

        logger.info("Preprocessing artifacts");
        var sourceElements = sourcePreprocessor.preprocess(sourceArtifacts);
        var targetElements = targetPreprocessor.preprocess(targetArtifacts);

        logger.info("Calculating embeddings");
        var sourceEmbeddings = embeddingCreator.calculateEmbeddings(sourceElements);
        var targetEmbeddings = embeddingCreator.calculateEmbeddings(targetElements);

        logger.info("Building element stores");
        sourceStore.setup(sourceElements, sourceEmbeddings);
        targetStore.setup(targetElements, targetEmbeddings);

        logger.info("Classifying Tracelinks");
        var llmResults = classifier.classify(sourceStore, targetStore);
        var traceLinks = aggregator.aggregate(sourceElements, targetElements, llmResults);

        logger.info("Postprocessing Tracelinks");
        traceLinks = traceLinkIdPostProcessor.postprocess(traceLinks);

        logger.info("Evaluating Results");
        Statistics.generateStatistics(
                traceLinks, configFile.toFile(), configuration, sourceArtifacts.size(), targetArtifacts.size());
        Statistics.saveTraceLinks(traceLinks, configFile.toFile(), configuration);

        CacheManager.getDefaultInstance().flush();

        return traceLinks;
    }

    /**
     * Gets the number of source artifacts in this evaluation.
     *
     * @return Number of source artifacts
     */
    public int getSourceArtifactCount() {
        return sourceArtifactProvider.getArtifacts().size();
    }

    /**
     * Gets the number of target artifacts in this evaluation.
     *
     * @return Number of target artifacts
     */
    public int getTargetArtifactCount() {
        return targetArtifactProvider.getArtifacts().size();
    }
}
