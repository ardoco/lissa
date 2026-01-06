/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.getTraceLinksFromGoldStandard;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.OptimizerConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.evaluator.Evaluator;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.MetricFactory;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.OptimizerFactory;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizer;

/**
 * Represents a single prompt optimization run of the LiSSA framework.
 * This class utilizes the general {@link Evaluation} pipeline and extends it by an optimization step at the end.
 * The pipeline adds these steps:
 * <ol>
 *     <li>Optimizes the prompt</li>
 * </ol>
 */
public class Optimization {

    private static final Logger LOGGER = LoggerFactory.getLogger(Optimization.class);
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
     *     <li>Creates the Metric, Evaluator and Optimizer</li>
     * </ol>
     *
     * @throws IOException If there are issues reading the configuration
     */
    private void setup() throws IOException {
        configuration = new ObjectMapper().readValue(configFile.toFile(), OptimizerConfiguration.class);
        evaluationPipeline = new Evaluation(configuration.evaluationConfiguration());
        Set<TraceLink> goldStandard = getTraceLinksFromGoldStandard(
                configuration.evaluationConfiguration().goldStandardConfiguration());

        Metric metric = MetricFactory.createScorer(
                configuration.metric(),
                evaluationPipeline.getClassifier(),
                evaluationPipeline.getAggregator(),
                evaluationPipeline.getTraceLinkIdPostProcessor());
        Evaluator evaluator = Evaluator.createEvaluator(configuration.evaluator());

        promptOptimizer =
                OptimizerFactory.createOptimizer(configuration.promptOptimizer(), goldStandard, metric, evaluator);
        configuration.serializeAndDestroyConfiguration();
    }

    /**
     * Runs the optimization pipeline.
     * This method:
     * <ol>
     *     <li>Sets up the source and target stores</li>
     *     <li>Optimizes the prompt using the configured optimizer</li>
     *     <li>Generates and saves optimization statistics</li>
     *     <li>Flushes the cache to persist changes</li>
     * </ol>
     *
     * @return The optimized prompt as a String
     */
    public String run() {
        evaluationPipeline.initializeSourceAndTargetStores();

        LOGGER.info("Optimizing Prompt");
        String result =
                promptOptimizer.optimize(evaluationPipeline.getSourceStore(), evaluationPipeline.getTargetStore());
        LOGGER.info("Optimized Prompt: {}", result);

        Statistics.generateOptimizationStatistics(configFile.toFile(), configuration, result);

        CacheManager.getDefaultInstance().flush();

        return result;
    }
}
