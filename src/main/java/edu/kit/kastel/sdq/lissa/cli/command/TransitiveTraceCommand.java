/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.cli.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.Statistics;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.GoldStandardConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

import picocli.CommandLine;

/**
 * Command implementation for performing transitive trace link analysis.
 * This command processes multiple configuration files sequentially to create and evaluate
 * transitive trace links between artifacts. It supports:
 * <ul>
 *     <li>Sequential processing of multiple configurations</li>
 *     <li>Transitive trace link calculation</li>
 *     <li>Optional evaluation against a gold standard</li>
 *     <li>Statistics generation and result storage</li>
 * </ul>
 *
 * The command can be invoked with:
 * <pre>
 * transitive -c config1.json config2.json [config3.json ...]
 * transitive -c config1.json config2.json -e gold_standard.json
 * </pre>
 */
@CommandLine.Command(
        name = "transitive",
        mixinStandardHelpOptions = true,
        description = "Invokes the pipeline (transitive trace link) and evaluates it")
public class TransitiveTraceCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TransitiveTraceCommand.class);

    /**
     * Array of configuration file paths to be processed sequentially for transitive trace link analysis.
     * This option requires at least two configuration paths to create transitive links.
     * The configurations will be processed in the order they are provided.
     */
    @CommandLine.Option(
            names = {"-c", "--configs"},
            arity = "2..*",
            description = "Specifies two or more config paths to be invoked sequentially.")
    private @Nullable Path[] transitiveTraceConfigs;

    /**
     * Path to the evaluation configuration file used for gold standard comparison.
     * This is optional - if not provided, only trace links will be generated without evaluation.
     * The gold standard should be a CSV file containing valid trace links for comparison.
     */
    @CommandLine.Option(
            names = {"-e", "--evaluation-config"},
            description = "Specifies the evaluation config path to be invoked after the transitive trace link.")
    private @Nullable Path evaluationConfig;

    /**
     * Executes the transitive trace link analysis pipeline.
     * This method:
     * <ol>
     *     <li>Validates the input configurations</li>
     *     <li>Processes each configuration sequentially</li>
     *     <li>Calculates transitive trace links</li>
     *     <li>Optionally evaluates results against a gold standard</li>
     *     <li>Generates and saves statistics</li>
     * </ol>
     *
     * The method handles various error conditions and provides appropriate logging.
     */
    @Override
    public void run() {
        if (transitiveTraceConfigs == null || transitiveTraceConfigs.length < 2) {
            logger.error("At least two config paths are required for transitive trace link");
            return;
        }

        GoldStandardConfiguration goldStandardConfiguration = GoldStandardConfiguration.load(evaluationConfig);

        if (evaluationConfig == null) {
            logger.warn("No evaluation config path provided, so we just produce the transitive trace links");
        } else if (goldStandardConfiguration == null) {
            logger.error("Loading evaluation config was not possible");
            return;
        }

        List<Evaluation> evaluations = new ArrayList<>();
        Queue<Set<TraceLink>> traceLinks = new ArrayDeque<>();
        createNontransitiveTraceLinks(evaluations, traceLinks);

        if (evaluations.size() != traceLinks.size()) {
            logger.error("Number of evaluations and trace link sets do not match");
            return;
        }

        Set<TraceLink> transitiveTraceLinks = calculateTransitiveTraceLinks(traceLinks);

        String key = createKey(evaluations, goldStandardConfiguration);
        Statistics.saveTraceLinks(transitiveTraceLinks, "transitive-trace-links_" + key + ".csv");

        if (goldStandardConfiguration != null) {
            int sourceArtifacts = evaluations.getFirst().getSourceArtifactCount();
            int targetArtifacts = evaluations.getLast().getTargetArtifactCount();
            Statistics.generateStatistics(
                    "transitive-trace-links_" + key,
                    joinConfigs(evaluations, goldStandardConfiguration),
                    transitiveTraceLinks,
                    goldStandardConfiguration,
                    sourceArtifacts,
                    targetArtifacts);
        }
    }

    /**
     * Calculates transitive trace links from a queue of trace link sets.
     * This method:
     * <ol>
     *     <li>Starts with the first set of trace links</li>
     *     <li>Iteratively combines with subsequent sets</li>
     *     <li>Creates new links where target of one link matches source of another</li>
     * </ol>
     *
     * @param traceLinks Queue of trace link sets to process
     * @return Set of transitive trace links
     */
    private Set<TraceLink> calculateTransitiveTraceLinks(Queue<Set<TraceLink>> traceLinks) {
        Set<TraceLink> transitiveTraceLinks = new LinkedHashSet<>(traceLinks.poll());
        while (!traceLinks.isEmpty()) {
            Set<TraceLink> currentLinks = transitiveTraceLinks;
            Set<TraceLink> nextLinks = traceLinks.poll();
            transitiveTraceLinks = new LinkedHashSet<>();
            logger.info("Joining trace links of size {} and {}", currentLinks.size(), nextLinks.size());
            for (TraceLink currentLink : currentLinks) {
                for (TraceLink nextLink : nextLinks) {
                    if (currentLink.targetId().equals(nextLink.sourceId())) {
                        transitiveTraceLinks.add(new TraceLink(currentLink.sourceId(), nextLink.targetId()));
                    }
                }
            }
            logger.info("Found transitive links of size {}", transitiveTraceLinks.size());
        }
        return transitiveTraceLinks;
    }

    /**
     * Creates non-transitive trace links by processing each configuration.
     * This method:
     * <ol>
     *     <li>Processes each configuration file sequentially</li>
     *     <li>Runs the evaluation pipeline for each configuration</li>
     *     <li>Collects the resulting trace links</li>
     * </ol>
     *
     * @param evaluations List to store evaluation instances
     * @param traceLinks Queue to store trace link sets
     */
    private void createNontransitiveTraceLinks(List<Evaluation> evaluations, Queue<Set<TraceLink>> traceLinks) {
        try {
            for (Path traceConfig : transitiveTraceConfigs) {
                logger.info("Invoking the pipeline with '{}'", traceConfig);
                Evaluation evaluation = new Evaluation(traceConfig);
                evaluations.add(evaluation);
                var traceLinksForRun = evaluation.run();
                logger.info("Found {} trace links", traceLinksForRun.size());
                traceLinks.add(traceLinksForRun);
            }
        } catch (IOException e) {
            logger.warn("Configuration threw an exception: {}", e.getMessage());
        }
    }

    /**
     * Joins configuration information from evaluations and gold standard.
     * This method:
     * <ol>
     *     <li>Collects configuration information from each evaluation</li>
     *     <li>Adds gold standard configuration if available</li>
     *     <li>Joins all configurations with newlines</li>
     * </ol>
     *
     * @param evaluations List of evaluation instances
     * @param goldStandardConfiguration Gold standard configuration if available
     * @return Joined configuration string
     */
    private String joinConfigs(List<Evaluation> evaluations, GoldStandardConfiguration goldStandardConfiguration) {
        List<String> evaluationConfigs = evaluations.stream()
                .map(Evaluation::getConfiguration)
                .map(Configuration::serializeAndDestroyConfiguration)
                .collect(Collectors.toCollection(ArrayList::new));

        if (goldStandardConfiguration != null) {
            evaluationConfigs.add(goldStandardConfiguration.toString());
        }

        return String.join("\n", evaluationConfigs);
    }

    /**
     * Creates a unique key for the current evaluation run.
     * This method:
     * <ol>
     *     <li>Joins all configuration information</li>
     *     <li>Generates a deterministic key from the joined configuration</li>
     * </ol>
     *
     * @param evaluations List of evaluation instances
     * @param goldStandardConfiguration Gold standard configuration if available
     * @return Generated key string
     */
    private String createKey(List<Evaluation> evaluations, GoldStandardConfiguration goldStandardConfiguration) {
        return KeyGenerator.generateKey(joinConfigs(evaluations, goldStandardConfiguration));
    }
}
