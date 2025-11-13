/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.Statistics;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.GoldStandardConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.utils.LogWriter;

/**
 * Optimizes the classification prompt used in LiSSA through iterative evaluations.
 * In each iteration, the optimizer:
 * - Runs an evaluation with the current configuration.
 * - Reads the resulting F1 score.
 * - Updates and stores the best prompt found so far.
 * - Asks the PromptWriter to generate an improved prompt if the target F1 has not yet been reached.
 * Optimized configurations are written to an optimized directory next to the original configuration file.
 */
public final class PromptOptimizer {
    private static final Logger logger = LoggerFactory.getLogger(PromptOptimizer.class);

    private final Path baseConfig;
    private final int maxIterations;
    private final double targetF1;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PromptWriter promptWriter;

    private double bestF1 = -1.0;
    private String bestPrompt = null;

    /**
     * Creates a new prompt optimizer for the given configuration file.
     *
     * @param baseConfig    path to the base configuration JSON file
     * @param maxIterations maximum number of optimization iterations
     * @param targetF1      target F1 score at which optimization should stop
     */
    public PromptOptimizer(Path baseConfig, int maxIterations, double targetF1) throws IOException {
        this.baseConfig = baseConfig;
        this.maxIterations = Math.max(1, maxIterations);
        this.targetF1 = targetF1;

        try {
            Configuration fullConfig = mapper.readValue(baseConfig.toFile(), Configuration.class);
            ModuleConfiguration llmModule = fullConfig.classifier();
            this.promptWriter = new PromptWriter(llmModule);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration for PromptWriter", e);
        }
    }

    /**
     * Runs the optimization loop until the target F1 score or the maximum
     * number of iterations is reached. Intermediate results and the best
     * prompt are logged to disk.
     */
    public void optimize() throws IOException {
        Path currentConfig = baseConfig;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            logger.info("=== Iteration {} ===", iteration);
            Evaluation eval = new Evaluation(currentConfig);
            Set<TraceLink> traceLinks = eval.run();
            double f1 = calculateF1FromEvaluation(eval, traceLinks);
            logger.info("Obtained F1 = {}", f1);

            int width = String.valueOf(maxIterations).length();
            String iterationString = String.format("%" + width + "d", iteration);
            LogWriter.write(
                    "f1_log.txt", "Iteration " + iterationString + " | F1 = " + String.format("%.4f", f1), true);

            String currentPrompt = readCurrentPrompt(currentConfig);

            if (f1 > bestF1) {
                bestF1 = f1;
                bestPrompt = currentPrompt;
            }

            if (f1 >= targetF1) {
                logger.info("Stopping criteria met: Target-F1-Score reached.");
                break;
            }
            if (iteration == maxIterations) {
                logger.info("Stopping criteria met: Maximum number of iterations reached.");
                break;
            }
            currentConfig = tweakPromptAndSave(iteration, currentConfig, currentPrompt, f1);
        }

        logger.info("=== Best Result ===");
        logger.info("Best F1 Score: {}", String.format("%.4f", bestF1));
        logger.info("Best Prompt:\n{}", bestPrompt);
        // Log Best Prompt
        LogWriter.write("best_prompt.txt", "Best F1: " + String.format("%.4f", bestF1) + "\n\n" + bestPrompt, false);
        LogWriter.write(
                "f1_log.txt",
                "Best F1: " + String.format("%.4f", bestF1)
                        + "\n----------------------------------------------------\n",
                true);
    }

    private double calculateF1FromEvaluation(Evaluation evaluation, Set<TraceLink> traceLinks) {
        GoldStandardConfiguration goldStandard = evaluation.getConfiguration().goldStandardConfiguration();
        Set<TraceLink> validTraceLinks = Statistics.getTraceLinksFromGoldStandard(goldStandard);

        var cmc = ClassificationMetricsCalculator.getInstance();
        var classification = cmc.calculateMetrics(traceLinks, validTraceLinks, null);

        return classification.getF1();
    }

    private String readCurrentPrompt(Path configPath) {
        try {
            Configuration config = mapper.readValue(configPath.toFile(), Configuration.class);
            return config.classifier().argumentAsString("template");
        } catch (IOException e) {
            logger.warn("Failed to read prompt from config {}", configPath, e);
            return "";
        }
    }

    /**
     * Creates and persists a new optimized configuration by replacing the current prompt
     * with an improved version suggested by the  PromptWriter.
     *
     * @param iteration      the current optimization iteration number
     * @param previousConfig the path to the configuration file from the last iteration
     * @param currentPrompt  the prompt used in the previous iteration
     * @return the path to the newly written optimized configuration file
     * @throws IOException if reading the previous configuration or writing the new one fails
     */
    private Path tweakPromptAndSave(int iteration, Path previousConfig, String currentPrompt, double lastF1)
            throws IOException {
        try {
            Configuration oldConfig = mapper.readValue(previousConfig.toFile(), Configuration.class);

            PromptGenerationResult result = promptWriter.improve(currentPrompt, iteration, lastF1);

            LogWriter.write(
                    "promptsToImprove_log.txt",
                    "=== Iteration " + iteration + " ===\nF1 last iteration: " + String.format("%.4f", lastF1) + "\n"
                            + result.aiPrompt() + "\n\n",
                    true);

            LogWriter.write(
                    "promptsToLissa_log.txt",
                    "=== Iteration " + iteration + " ===\nF1 last iteration: " + String.format("%.4f", lastF1) + "\n"
                            + result.lissaPrompt() + "\n\n",
                    true);

            // Replace the "template" argument in the classifier with the improved LiSSA prompt
            ModuleConfiguration oldClassifier = oldConfig.classifier();
            SortedMap<String, String> newArguments = new TreeMap<>(oldClassifier.arguments());
            newArguments.put("template", result.lissaPrompt());

            ModuleConfiguration newClassifier = new ModuleConfiguration(oldClassifier.name(), newArguments);
            Configuration newConfig = oldConfig.withReplacedClassifier(newClassifier);

            // Ensure optimized/ directory exists
            Path optimizedDir = baseConfig.getParent().resolve("optimized");
            Files.createDirectories(optimizedDir);

            // Generate the output file path for the next iteration
            String baseName = baseConfig.getFileName().toString().replace(".json", "");
            Path nextConfig = optimizedDir.resolve(baseName + "_opt" + iteration + ".json");

            mapper.writerWithDefaultPrettyPrinter().writeValue(nextConfig.toFile(), newConfig);
            return nextConfig;

        } catch (IOException e) {
            throw new IOException("Writing optimized config failed", e);
        }
    }
}
