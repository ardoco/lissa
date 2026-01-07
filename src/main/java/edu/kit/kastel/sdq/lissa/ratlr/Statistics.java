/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.GoldStandardConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.OptimizerConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

/**
 * Utility class for generating and saving statistics about trace link analysis results.
 * This class provides functionality to:
 * <ul>
 *     <li>Calculate classification metrics (precision, recall, F1)</li>
 *     <li>Generate detailed statistics reports</li>
 *     <li>Save trace links to CSV files</li>
 *     <li>Compare results against gold standards</li>
 * </ul>
 *
 * The statistics include:
 * <ul>
 *     <li>Number of trace links in gold standard</li>
 *     <li>Number of source and target artifacts</li>
 *     <li>True positives, false positives, and false negatives</li>
 *     <li>Precision, recall, and F1 scores</li>
 * </ul>
 */
public final class Statistics {
    private static final Logger logger = LoggerFactory.getLogger(Statistics.class);

    private Statistics() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Generates statistics for trace link analysis results.
     * This method:
     * <ol>
     *     <li>Loads the gold standard trace links</li>
     *     <li>Calculates classification metrics</li>
     *     <li>Generates a detailed report</li>
     *     <li>Saves the report to a markdown file</li>
     * </ol>
     *
     * @param traceLinks Set of identified trace links
     * @param configFile Configuration file used for the analysis
     * @param configuration Configuration object used for the analysis
     * @param sourceArtifacts Number of source artifacts
     * @param targetArtifacts Number of target artifacts
     * @throws UncheckedIOException If there are issues writing the statistics file
     */
    public static void generateStatistics(
            Set<TraceLink> traceLinks,
            File configFile,
            Configuration configuration,
            int sourceArtifacts,
            int targetArtifacts)
            throws UncheckedIOException {
        generateStatistics(
                configuration.getConfigurationIdentifierForFile(configFile.getName()),
                configuration.serializeAndDestroyConfiguration(),
                traceLinks,
                configuration.goldStandardConfiguration(),
                sourceArtifacts,
                targetArtifacts);
    }

    /**
     * Generates statistics for trace link analysis results with custom configuration identifier.
     * This method:
     * <ol>
     *     <li>Validates the gold standard configuration</li>
     *     <li>Loads valid trace links from the gold standard</li>
     *     <li>Calculates classification metrics</li>
     *     <li>Generates a detailed report with configuration and results</li>
     *     <li>Saves the report to a markdown file</li>
     * </ol>
     *
     * @param configurationIdentifier Unique identifier for the configuration
     * @param configurationSummary Summary of the configuration used
     * @param traceLinks Set of identified trace links
     * @param goldStandardConfiguration Gold standard configuration for comparison
     * @param sourceArtifacts Number of source artifacts
     * @param targetArtifacts Number of target artifacts
     * @throws UncheckedIOException If there are issues writing the statistics file
     */
    public static void generateStatistics(
            String configurationIdentifier,
            String configurationSummary,
            Set<TraceLink> traceLinks,
            @Nullable GoldStandardConfiguration goldStandardConfiguration,
            int sourceArtifacts,
            int targetArtifacts)
            throws UncheckedIOException {

        if (goldStandardConfiguration == null || goldStandardConfiguration.path() == null) {
            logger.info(
                    "Skipping statistics generation since no path to ground truth has been provided as first command line argument");
            return;
        }

        Set<TraceLink> validTraceLinks = getTraceLinksFromGoldStandard(goldStandardConfiguration);

        ClassificationMetricsCalculator cmc = ClassificationMetricsCalculator.getInstance();
        var classification = cmc.calculateMetrics(traceLinks, validTraceLinks, null);
        classification.prettyPrint();

        // Store information to one file (config and results)
        var resultFile = new File("results-" + configurationIdentifier + ".md");
        StringBuilder result = new StringBuilder();
        result.append(configurationToString(configurationIdentifier, configurationSummary));
        result.append("## Stats\n");
        result.append("* #TraceLinks (GS): ").append(validTraceLinks.size()).append("\n");
        result.append("* #Source Artifacts: ").append(sourceArtifacts).append("\n");
        result.append("* #Target Artifacts: ").append(targetArtifacts).append("\n");
        result.append("## Results\n");
        result.append("* True Positives: ")
                .append(classification.getTruePositives().size())
                .append("\n");
        result.append("* False Positives: ")
                .append(classification.getFalsePositives().size())
                .append("\n");
        result.append("* False Negatives: ")
                .append(classification.getFalseNegatives().size())
                .append("\n");
        result.append("* Precision: ").append(classification.getPrecision()).append("\n");
        result.append("* Recall: ").append(classification.getRecall()).append("\n");
        result.append("* F1: ").append(classification.getF1()).append("\n");

        logger.info("Storing results to {}", resultFile.getName());
        try {
            Files.writeString(resultFile.toPath(), result.toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads trace links from a gold standard file.
     * This method:
     * <ol>
     *     <li>Reads the gold standard file</li>
     *     <li>Skips header if configured</li>
     *     <li>Parses each line into a trace link</li>
     *     <li>Handles column swapping if configured</li>
     * </ol>
     *
     * @param goldStandardConfiguration Configuration for the gold standard file
     * @return Set of valid trace links from the gold standard
     * @throws UncheckedIOException If there are issues reading the gold standard file
     */
    public static Set<TraceLink> getTraceLinksFromGoldStandard(GoldStandardConfiguration goldStandardConfiguration) {
        File groundTruth = new File(goldStandardConfiguration.path());
        boolean header = goldStandardConfiguration.hasHeader();
        logger.info("Skipping header: {}", header);
        Set<TraceLink> validTraceLinks;
        try {
            validTraceLinks = Files.readAllLines(groundTruth.toPath()).stream()
                    .skip(header ? 1 : 0)
                    .map(l -> l.split(","))
                    .map(it -> goldStandardConfiguration.swapColumns()
                            ? new TraceLink(it[1], it[0])
                            : new TraceLink(it[0], it[1]))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return validTraceLinks;
    }

    /**
     * Saves trace links to a CSV file based on configuration.
     * This method:
     * <ol>
     *     <li>Generates a filename based on the configuration</li>
     *     <li>Delegates to the main save method</li>
     * </ol>
     *
     * @param traceLinks Set of trace links to save
     * @param configFile Configuration file used for the analysis
     * @param configuration Configuration object used for the analysis
     * @throws UncheckedIOException If there are issues writing the trace links file
     */
    public static void saveTraceLinks(Set<TraceLink> traceLinks, File configFile, Configuration configuration)
            throws UncheckedIOException {
        var fileName = "traceLinks-" + configuration.getConfigurationIdentifierForFile(configFile.getName()) + ".csv";
        saveTraceLinks(traceLinks, fileName);
    }

    /**
     * Saves trace links to a CSV file.
     * This method:
     * <ol>
     *     <li>Sorts trace links by source and target IDs</li>
     *     <li>Converts trace links to CSV format</li>
     *     <li>Writes the result to the specified file</li>
     * </ol>
     *
     * @param traceLinks Set of trace links to save
     * @param destination Path to the output file
     * @throws UncheckedIOException If there are issues writing the trace links file
     */
    public static void saveTraceLinks(Set<TraceLink> traceLinks, String destination) throws UncheckedIOException {
        logger.info("Storing trace links to {}", destination);

        List<TraceLink> orderedTraceLinks = new ArrayList<>(traceLinks);
        orderedTraceLinks.sort(Comparator.comparing(TraceLink::sourceId).thenComparing(TraceLink::targetId));

        String csvResult = orderedTraceLinks.stream()
                .map(it -> it.sourceId() + "," + it.targetId())
                .collect(Collectors.joining("\n"));
        try {
            Files.writeString(new File(destination).toPath(), csvResult, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Generates statistics for prompt optimization.
     *
     * This method:
     * <ol>
     *     <li>Generates a detailed report with configuration and results</li>
     *     <li>Saves the report to a markdown file</li>
     * </ol>
     * @param configFile Configuration file used for the optimization
     * @param configuration Configuration object used for the optimization
     * @param prompt Optimized prompt generated during the optimization
     * @throws UncheckedIOException If there are issues writing the statistics file
     */
    public static void generateOptimizationStatistics(
            File configFile, OptimizerConfiguration configuration, String prompt) throws UncheckedIOException {
        generateOptimizationStatistics(
                configuration.getConfigurationIdentifierForFile(configFile.getName()),
                configuration.serializeAndDestroyConfiguration(),
                prompt);
    }

    /**
     * Generates statistics for prompt optimization with a specific iteration count.
     * This method modifies the maximum_iterations field in the serialized configuration JSON
     * to reflect the actual iteration number.
     *
     * @param configFile Configuration file used for the optimization
     * @param configurationSummary Already serialized configuration JSON
     * @param prompt Optimized prompt generated during the optimization
     * @param iterationCount The actual iteration count to set in the configuration
     * @throws UncheckedIOException If there are issues writing the statistics file
     */
    public static void generateOptimizationStatistics(
            File configFile, String configurationSummary, String prompt, int iterationCount)
            throws UncheckedIOException {
        // Modify the maximum_iterations in the serialized JSON
        String modifiedConfig = modifyMaxIterationsInJson(configurationSummary, iterationCount);

        // Generate a unique identifier for this iteration
        String configurationIdentifier = configFile.getName() + "_iter" + iterationCount;

        generateOptimizationStatistics(configurationIdentifier, modifiedConfig, prompt);
    }

    /**
     * Modifies the maximum_iterations field in a JSON configuration string.
     *
     * @param jsonConfig The original JSON configuration string
     * @param iterationCount The new iteration count to set
     * @return The modified JSON configuration string
     */
    private static String modifyMaxIterationsInJson(String jsonConfig, int iterationCount) {
        // Use regex to replace the maximum_iterations value
        return jsonConfig.replaceAll(
                "\"maximum_iterations\"\\s*:\\s*\"?\\d+\"?", "\"maximum_iterations\" : \"" + iterationCount + "\"");
    }

    /**
     * Generates statistics for prompt optimization with custom configuration identifier.
     *
     * This method:
     * <ol>
     *     <li>Generates a detailed report with configuration and results</li>
     *     <li>Saves the report to a markdown file</li>
     * </ol>
     *
     * @param configurationIdentifier Unique identifier for the configuration
     * @param configurationSummary Summary of the configuration used
     * @param prompt Optimized prompt used in the analysis
     * @throws UncheckedIOException If there are issues writing the statistics file
     */
    public static void generateOptimizationStatistics(
            String configurationIdentifier, String configurationSummary, String prompt) throws UncheckedIOException {

        // Store information to one file (config and results)
        var resultFile = new File("results-prompt-optimization-" + configurationIdentifier + ".md");
        StringBuilder result = new StringBuilder();
        result.append(configurationToString(configurationIdentifier, configurationSummary));
        result.append("## Stats\n");
        result.append(" * Optimized Prompt: ").append(escapeMarkdown(prompt)).append("\n");

        logger.info("Storing results to {}", resultFile.getName());
        try {
            Files.writeString(resultFile.toPath(), result.toString(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String configurationToString(String configurationIdentifier, String configurationSummary) {
        return "## Configuration ("
                + new SimpleDateFormat("yyyy-MM-dd_HH-mmZZZ").format(new Date())
                + " -- "
                + configurationIdentifier
                + ")\n```json\n"
                + configurationSummary
                + "\n```\n\n";
    }

    /**
     * Escapes special characters in a string for Markdown formatting.
     * This method ensures that characters like `*`, `_`, and `#` are escaped
     * to prevent them from being interpreted as Markdown syntax.
     *
     * @param text The input text to escape
     * @return The escaped text suitable for Markdown
     */
    public static String escapeMarkdown(String text) {
        boolean needsCodeBlock = text.contains("\n") || text.length() > 80;

        // List of Markdown special characters to escape
        String markdownSpecialChars = "\\`*_{}[]()#+-.!|>";

        // Escape each special character
        StringBuilder escaped = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (markdownSpecialChars.indexOf(c) != -1) {
                escaped.append('\\');
            }
            escaped.append(c);
        }

        if (needsCodeBlock) {
            return "\n```\n" + escaped + "\n```";
        }
        return escaped.toString();
    }
}
