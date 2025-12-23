/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * An abstract implementation of the Metric interface that provides a framework for evaluating classification tasks
 * using a specified classifier and result aggregator. Subclasses must implement the reduce method to define how
 * the classified results are reduced to a single score.
 * Metrics of this type do not compute individual scores for each classification task, but rather depend on the set
 * of tasks as a whole.
 */
public abstract class GlobalMetric implements Metric {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMetric.class);

    private final Classifier classifier;
    private final ResultAggregator aggregator;
    private final boolean usesCustomAggregator;
    private final TraceLinkIdPostprocessor postprocessor;

    protected GlobalMetric(Classifier classifier, ResultAggregator aggregator, TraceLinkIdPostprocessor postprocessor) {
        this.classifier = classifier;
        this.aggregator = aggregator;
        this.usesCustomAggregator = aggregator != null;
        this.postprocessor = postprocessor;
    }

    /**
     * This method computes scores for a list of prompts and classification task examples.
     * Each prompt is delegated to the {@link #getMetric(String, List)} method for scoring with the entire example set.
     */
    @Override
    public List<Double> getMetric(List<String> prompts, List<ClassificationTask> examples) {
        List<Double> scores = new ArrayList<>();
        for (String prompt : prompts) {
            scores.add(getMetric(prompt, examples));
        }
        return scores;
    }

    /**
     * This method computes the metric for a single prompt against a list of classification tasks.
     * It classifies the examples using the specified classifier and aggregates the results into accepted and rejected sets.
     * The final metric value is computed by reducing the classified results against the ground truth using the
     * abstract {@link #reduce(Collection, Collection, Collection)} method.
     */
    @Override
    public Double getMetric(String prompt, List<ClassificationTask> examples) {
        if (examples.size() > 1) {
            logger.debug("Computing metric for tasks: {}", examples);
        }

        Pair<Set<TraceLink>, Set<TraceLink>> classifiedLinks = classify(prompt, examples);

        if (examples.size() > 1) {
            logger.debug(
                    "Results: {} accepted, {} rejected",
                    classifiedLinks.first().size(),
                    classifiedLinks.second().size());
        }

        Set<TraceLink> groundTruth = examples.stream()
                .filter(ClassificationTask::label)
                .map(task -> TraceLink.of(
                        task.source().getIdentifier(), task.target().getIdentifier()))
                .collect(Collectors.toSet());

        Double score = reduce(classifiedLinks.first(), classifiedLinks.second(), groundTruth);

        if (examples.size() > 1) {
            logger.debug("Score: {}", score);
        }

        return score;
    }

    /**
     * Reduces the given collections of items, rejected items, and ground truth into a single score.
     * The specific reduction strategy is defined in implementations.
     *
     * @param items The collection of items considered as accepted.
     * @param rejectedItems The collection of items considered as rejected.
     * @param groundTruth The collection of ground truth items for comparison.
     * @return A double representing the reduced score.
     * @param <T> The type of items being reduced.
     */
    protected abstract <T> double reduce(Collection<T> items, Collection<T> rejectedItems, Collection<T> groundTruth);

    /**
     * Classifies the given tasks using the specified prompt and aggregates the results into sets of accepted and rejected trace links.
     *
     * @param prompt The prompt to use for classification.
     * @param tasks The collection of classification tasks to be classified.
     * @return A pair containing two sets of trace links: the first set contains accepted links, and the second set contains rejected links.
     */
    private Pair<Set<TraceLink>, Set<TraceLink>> classify(String prompt, Collection<ClassificationTask> tasks) {
        if (tasks.size() > 1) {
            logger.debug("=== Starting classification for {} tasks ===", tasks.size());
        }
        classifier.setClassificationPrompt(prompt);
        List<ClassificationResult> acceptedTraceLinks = new ArrayList<>();
        List<ClassificationResult> rejectedTraceLinks = new ArrayList<>();

        int taskIndex = 0;
        for (ClassificationTask task : tasks) {
            if (tasks.size() > 1) {
                logger.debug(
                        "Task {}/{}: {} -> {} (label: {})",
                        ++taskIndex,
                        tasks.size(),
                        task.source().getIdentifier(),
                        task.target().getIdentifier(),
                        task.label());
            }

            Optional<ClassificationResult> result = classifier.classify(task);
            if (result.isPresent()) {
                if (tasks.size() > 1) {
                    logger.debug(
                            "  -> ACCEPTED with confidence: {}", result.get().confidence());
                }
                acceptedTraceLinks.add(result.get());
            } else {
                if (tasks.size() > 1) {
                    logger.debug("  -> REJECTED (empty result)");
                }
                // TODO: Is there a constant for use instead of 0.0?
                rejectedTraceLinks.add(new ClassificationResult(task.source(), task.target(), 0.0));
            }
        }

        if (tasks.size() > 1) {
            logger.debug(
                    "=== Classification summary: {} accepted, {} rejected ===",
                    acceptedTraceLinks.size(),
                    rejectedTraceLinks.size());
        }

        return new Pair<>(aggregate(acceptedTraceLinks), aggregate(rejectedTraceLinks));
    }

    /**
     * Aggregates the classification results into a set of trace links using either a custom aggregator or a default method.
     *
     * @param classificationResults The list of classification results to be aggregated.
     * @return A set of trace links derived from the classification results.
     */
    private Set<TraceLink> aggregate(List<ClassificationResult> classificationResults) {
        if (!usesCustomAggregator) {
            return defaultAggregator(classificationResults);
        }
        List<Element> sourceElements = new ArrayList<>();
        List<Element> targetElements = new ArrayList<>();
        for (ClassificationResult result : classificationResults) {
            sourceElements.add(result.source());
            targetElements.add(result.target());
        }
        Set<TraceLink> aggregatedTraceLinks =
                aggregator.aggregate(sourceElements, targetElements, classificationResults);
        return postprocessor.postprocess(aggregatedTraceLinks);
    }

    /**
     * Default method to aggregate classification results into trace links.
     * Each classification result is converted into a trace link using the identifiers of the source and target elements.
     * TODO: consider extracting into a aggregator implementation ???
     *
     * @param classificationResults The list of classification results to be aggregated.
     * @return A set of trace links derived from the classification results.
     */
    private static Set<TraceLink> defaultAggregator(List<ClassificationResult> classificationResults) {
        return classificationResults.stream()
                .map(result -> TraceLink.of(
                        result.source().getIdentifier(), result.target().getIdentifier()))
                .collect(Collectors.toSet());
    }
}
