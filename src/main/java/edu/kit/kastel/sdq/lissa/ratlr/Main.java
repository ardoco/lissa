/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.ArtifactProvider;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator.EmbeddingCreator;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;

/**
 * Main entry point for the LiSSA framework's core functionality (only for testing purposes, use the CLI for production).
 * This class orchestrates the trace link analysis pipeline, including:
 * <ul>
 *     <li>Loading and preprocessing artifacts</li>
 *     <li>Calculating embeddings</li>
 *     <li>Building element stores</li>
 *     <li>Classifying trace links</li>
 *     <li>Postprocessing results</li>
 *     <li>Generating statistics</li>
 * </ul>
 * <p>
 * The pipeline uses a {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} to share context objects
 * between components such as artifact providers, preprocessors, embedding creators, classifiers, and aggregators.
 * </p>
 *
 * The pipeline follows these steps:
 * <ol>
 *     <li>Load configuration from file</li>
 *     <li>Initialize components (providers, preprocessors, etc.)</li>
 *     <li>Load artifacts from source and target providers</li>
 *     <li>Preprocess artifacts into elements</li>
 *     <li>Calculate embeddings for elements</li>
 *     <li>Build element stores for efficient access</li>
 *     <li>Classify potential trace links</li>
 *     <li>Aggregate results into final trace links</li>
 *     <li>Postprocess trace link IDs</li>
 *     <li>Generate and save statistics</li>
 * </ol>
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Main entry point for the LiSSA framework.
     * This method:
     * <ol>
     *     <li>Loads configuration from file (defaults to "config.json" if no file specified)</li>
     *     <li>Initializes all required components, sharing a {@link ContextStore}</li>
     *     <li>Executes the complete trace link analysis pipeline</li>
     *     <li>Generates and saves results</li>
     * </ol>
     *
     * @param args Command line arguments. If no arguments are provided, uses "config.json" as the default configuration file.
     * @throws IOException If there are issues reading the configuration file or processing artifacts
     */
    public static void main(String[] args) throws IOException {
        var configFilePath = args.length == 0 ? "config.json" : args[0];
        var configFile = new File(configFilePath);
        Configuration configuration = new ObjectMapper().readValue(configFile, Configuration.class);
        CacheManager.setCacheDir(configuration.cacheDir());

        ContextStore contextStore = new ContextStore();

        ArtifactProvider sourceArtifactProvider =
                ArtifactProvider.createArtifactProvider(configuration.sourceArtifactProvider(), contextStore);
        ArtifactProvider targetArtifactProvider =
                ArtifactProvider.createArtifactProvider(configuration.targetArtifactProvider(), contextStore);

        Preprocessor sourcePreprocessor =
                Preprocessor.createPreprocessor(configuration.sourcePreprocessor(), contextStore);
        Preprocessor targetPreprocessor =
                Preprocessor.createPreprocessor(configuration.targetPreprocessor(), contextStore);

        EmbeddingCreator embeddingCreator =
                EmbeddingCreator.createEmbeddingCreator(configuration.embeddingCreator(), contextStore);
        SourceElementStore sourceStore = new SourceElementStore(configuration.sourceStore());
        TargetElementStore targetStore = new TargetElementStore(configuration.targetStore());

        Classifier classifier = configuration.createClassifier(contextStore);
        ResultAggregator aggregator =
                ResultAggregator.createResultAggregator(configuration.resultAggregator(), contextStore);

        TraceLinkIdPostprocessor traceLinkIdPostProcessor = TraceLinkIdPostprocessor.createTraceLinkIdPostprocessor(
                configuration.traceLinkIdPostprocessor(), contextStore);

        configuration.serializeAndDestroyConfiguration();

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
                traceLinks, configFile, configuration, sourceArtifacts.size(), targetArtifacts.size());
        Statistics.saveTraceLinks(traceLinks, configFile, configuration);

        CacheManager.getDefaultInstance().flush();
    }
}
