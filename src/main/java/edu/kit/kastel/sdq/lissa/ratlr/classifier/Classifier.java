/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.ElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * Abstract base class for trace link classifiers in the LiSSA framework.
 * This class provides the foundation for implementing different classification strategies
 * for identifying trace links between source and target elements. It supports both
 * sequential and parallel processing of classification tasks.
 */
public abstract class Classifier {
    /**
     * Separator used in configuration names.
     */
    public static final String CONFIG_NAME_SEPARATOR = "_";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final int threads;

    /**
     * Creates a new classifier with the specified number of threads.
     *
     * @param threads The number of threads to use for parallel processing
     */
    protected Classifier(int threads) {
        this.threads = Math.max(1, threads);
    }

    /**
     * Creates a list of classification tasks from source and target element stores.
     * Each task represents a pair of elements to be classified.
     *
     * @param sourceStore The store containing source elements
     * @param targetStore The store containing target elements
     * @return A list of element pairs to classify
     */
    protected static List<Pair<Element, Element>> createClassificationTasks(
            ElementStore sourceStore, ElementStore targetStore) {
        List<Pair<Element, Element>> tasks = new ArrayList<>();

        for (var source : sourceStore.getAllElements(true)) {
            var targetCandidates = targetStore.findSimilar(source.second());
            for (Element target : targetCandidates) {
                tasks.add(new Pair<>(source.first(), target));
            }
        }
        return tasks;
    }

    /**
     * Creates a classifier instance based on the provided configuration.
     * The type of classifier is determined by the first part of the configuration name.
     *
     * @param configuration The module configuration for the classifier
     * @return A new classifier instance
     * @throws IllegalStateException If the configuration name is not recognized
     */
    public static Classifier createClassifier(ModuleConfiguration configuration) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case "mock" -> new MockClassifier();
            case "simple" -> new SimpleClassifier(configuration);
            case "reasoning" -> new ReasoningClassifier(configuration);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    /**
     * Creates a multi-stage classifier that processes elements through a pipeline of classifiers.
     * Each stage in the pipeline can have multiple configurations that are processed in sequence.
     *
     * @param configs A list of configuration lists, where each inner list represents a stage
     * @return A new pipeline classifier instance
     */
    public static Classifier createMultiStageClassifier(List<List<ModuleConfiguration>> configs) {
        return new PipelineClassifier(configs);
    }

    /**
     * Classifies trace links between source and target elements.
     * This method can process the classification either sequentially or in parallel
     * depending on the number of threads configured.
     *
     * @param sourceStore The store containing source elements
     * @param targetStore The store containing target elements
     * @return A list of classification results
     */
    public List<ClassificationResult> classify(ElementStore sourceStore, ElementStore targetStore) {
        var tasks = createClassificationTasks(sourceStore, targetStore);

        if (threads <= 1) {
            return sequentialClassify(tasks);
        }
        return parallelClassify(tasks);
    }

    /**
     * Performs parallel classification of trace links using virtual threads.
     * Each thread processes tasks from a shared queue and adds results to a concurrent collection.
     *
     * @param tasks The list of element pairs to classify
     * @return A list of classification results
     */
    protected final List<ClassificationResult> parallelClassify(List<Pair<Element, Element>> tasks) {
        ConcurrentLinkedQueue<ClassificationResult> results = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Pair<Element, Element>> taskQueue = new ConcurrentLinkedQueue<>(tasks);

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = Thread.ofVirtual().start(new Runnable() {
                private final Classifier copy = copyOf();

                @Override
                public void run() {
                    while (!taskQueue.isEmpty()) {
                        Pair<Element, Element> pair = taskQueue.poll();
                        if (pair == null) {
                            return;
                        }
                        var result = copy.classify(pair.first(), pair.second());
                        logger.debug(
                                "Classified (P) {} with {}: {}",
                                pair.first().getIdentifier(),
                                pair.second().getIdentifier(),
                                result);
                        result.ifPresent(results::add);
                    }
                }
            });
        }

        logger.info("Waiting for classification to finish. Tasks in queue: {}", taskQueue.size());

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                logger.error("Worker thread interrupted.", e);
                Thread.currentThread().interrupt();
            }
        }

        List<ClassificationResult> resultList = new ArrayList<>(results);
        logger.info("Finished parallel classification with {} results.", resultList.size());
        return resultList;
    }

    /**
     * Performs sequential classification of trace links.
     * Each element pair is processed one at a time in the current thread.
     *
     * @param tasks The list of element pairs to classify
     * @return A list of classification results
     */
    private List<ClassificationResult> sequentialClassify(List<Pair<Element, Element>> tasks) {
        List<ClassificationResult> results = new ArrayList<>();
        for (var task : tasks) {
            var result = classify(task.first(), task.second());
            logger.debug(
                    "Classified {} with {}: {}",
                    task.first().getIdentifier(),
                    task.second().getIdentifier(),
                    result);
            result.ifPresent(results::add);
        }
        logger.info("Finished sequential classification with {} results.", results.size());
        return results;
    }

    /**
     * Classifies a pair of elements.
     * This method must be implemented by concrete classifier implementations to define
     * their specific classification logic.
     *
     * @param source The source element
     * @param target The target element
     * @return A classification result if a trace link is found, empty otherwise
     */
    protected abstract Optional<ClassificationResult> classify(Element source, Element target);

    /**
     * Creates a copy of this classifier instance.
     * This method is used to create thread-local copies for parallel processing.
     *
     * @return A new instance of the same classifier type
     */
    protected abstract Classifier copyOf();

    /**
     * Gets the current prompt template used by the classifier, if any.
     * Default: empty string
     *
     * @return the current prompt text
     */
    public String getPrompt() {
        return "";
    }

    /**
     * Sets a new prompt template for the classifier.
     * Default: does nothing
     *
     * @param prompt the new prompt text
     */
    public void setPrompt(String prompt) {
        // default: ignore
    }
}
