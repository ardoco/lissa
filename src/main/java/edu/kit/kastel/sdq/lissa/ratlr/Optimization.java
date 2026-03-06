/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.getTraceLinksFromGoldStandard;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.OptimizerConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizer;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector.Selector;

/**
 * Represents a single prompt optimization run of the LiSSA framework.
 * This class utilizes the general {@link Evaluation} pipeline and extends it by an optimization step at the end.
 * The pipeline adds these steps:
 * <ol>
 *     <li>Optimizes the prompt</li>
 * </ol>
 */
public class Optimization {

    private static final Logger logger = LoggerFactory.getLogger(Optimization.class);
    private final Path configFile;

    private OptimizerConfiguration configuration;

    /**
     * The evaluation pipeline used for the optimization.
     * This pipeline includes all steps from artifact provision to trace link classification.
     */
    private Evaluation evaluationPipeline;
    /**
     * Optimizer for prompt used in classification
     */
    private PromptOptimizer promptOptimizer;

    /**
     * Creates a new evaluation instance with the specified configuration file.
     * This constructor:
     * <ol>
     *     <li>Validates the configuration file path</li>
     *     <li>Loads and initializes the configuration</li>
     *     <li>Sets up all required components for the pipeline</li>
     * </ol>
     *
     * @param configFile Path to the configuration file
     * @throws IOException          If there are issues reading the configuration file
     * @throws NullPointerException If configFile is null
     */
    public Optimization(Path configFile) throws IOException {
        this.configFile = Objects.requireNonNull(configFile);
        setup();
    }

    /**
     * Sets up the optimization pipeline by loading the configuration and initializing all required components.
     * This method:
     * <ol>
     *     <li>Loads the configuration from the specified file</li>
     *     <li>Initializes the evaluation pipeline</li>
     *     <li>Creates the Metric, Selector and Optimizer</li>
     * </ol>
     *
     * @throws IOException If there are issues reading the configuration
     */
    private void setup() throws IOException {
        configuration = new ObjectMapper().readValue(configFile.toFile(), OptimizerConfiguration.class);
        evaluationPipeline = new Evaluation(configuration.evaluationConfiguration());
        Set<TraceLink> goldStandard = getTraceLinksFromGoldStandard(
                configuration.evaluationConfiguration().goldStandardConfiguration());

        Metric metric = Metric.createMetric(
                configuration.metric(),
                evaluationPipeline.getClassifier(),
                evaluationPipeline.getAggregator(),
                evaluationPipeline.getTraceLinkIdPostProcessor());
        Selector selector = Selector.createSelector(configuration.selector());

        promptOptimizer =
                PromptOptimizer.createOptimizer(configuration.promptOptimizer(), goldStandard, metric, selector);
        configuration.serializeAndDestroyConfiguration();
    }

    /**
     * Runs the optimization pipeline.
     * This method:
     * <ol>
     *     <li>Sets up the source and target stores</li>
     *     <li>Optimizes the prompt using the configured optimizer</li>
     *     <li>Generates and saves optimization statistics for each intermediate prompt</li>
     *     <li>Generates and saves optimization statistics for the final prompt</li>
     *     <li>Flushes the cache to persist changes</li>
     * </ol>
     *
     * @return An list of prompts representing the optimization state at each iteration,
     *         where the last element is the final optimized prompt
     */
    public List<String> run() {
        evaluationPipeline.initializeSourceAndTargetStores();

        logger.info("Optimizing Prompt");

        List<String> results =
                promptOptimizer.optimize(evaluationPipeline.getSourceStore(), evaluationPipeline.getTargetStore());

        if (results.isEmpty()) {
            logger.warn("No optimized prompt was generated. Make sure maximum_iterations is set to greater than zero.");
            return results;
        }

        String configurationSummary = configuration.serializeAndDestroyConfiguration();

        for (int i = 0; i < results.size(); i++) {
            Statistics.generateOptimizationStatistics(configFile.toFile(), configurationSummary, results.get(i), i + 1);
        }
        logger.info("Optimized prompt after {} steps: \n {}", results.size(), results.getLast());

        CacheManager.getDefaultInstance().flush();

        return results;
    }
}
