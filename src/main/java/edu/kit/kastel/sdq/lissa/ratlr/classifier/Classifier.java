/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import static edu.kit.kastel.sdq.lissa.ratlr.classifier.MockClassifier.MOCK_CLASSIFIER_NAME;
import static edu.kit.kastel.sdq.lissa.ratlr.classifier.ReasoningClassifier.REASONING_CLASSIFIER_NAME;
import static edu.kit.kastel.sdq.lissa.ratlr.classifier.SimpleClassifier.SIMPLE_CLASSIFIER_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * Abstract base class for trace link classifiers in the LiSSA framework.
 * This class provides the foundation for implementing different classification strategies
 * for identifying trace links between source and target elements. It supports both
 * sequential and parallel processing of classification tasks.
 * <p>
 * All classifiers have access to a shared {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} via the protected {@code contextStore} field,
 * which is initialized in the constructor and available to all subclasses.
 * Subclasses should not duplicate context handling.
 * </p>
 */
public abstract class Classifier {
    /**
     * Separator used in configuration names.
     */
    public static final String CONFIG_NAME_SEPARATOR = "_";

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final int threads;
    /**
     * The shared context store for pipeline components.
     * Available to all subclasses for accessing shared context.
     */
    protected final ContextStore contextStore;

    /**
     * Creates a new classifier with the specified number of threads and context store.
     *
     * @param threads The number of threads to use for parallel processing
     * @param contextStore The shared context store for pipeline components
     */
    protected Classifier(int threads, ContextStore contextStore) {
        this.threads = Math.max(1, threads);
        this.contextStore = Objects.requireNonNull(contextStore);
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
    public List<ClassificationResult> classify(SourceElementStore sourceStore, TargetElementStore targetStore) {
        return classify(createClassificationTasks(sourceStore, targetStore));
    }

    private List<ClassificationResult> classify(List<Pair<Element, Element>> tasks) {
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

        logger.debug("Waiting for classification to finish. Tasks in queue: {}", taskQueue.size());

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                logger.error("Worker thread interrupted.", e);
                Thread.currentThread().interrupt();
            }
        }

        List<ClassificationResult> resultList = new ArrayList<>(results);
        logger.debug("Finished parallel classification with {} results.", resultList.size());
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
     * Classifies a single classification task.
     * This method delegates to the abstract {@link #classify(Element, Element)} method
     * which must be implemented by concrete classifier subclasses.
     *
     * @param task The classification task containing source and target elements
     * @return     A classification result if a trace link is found, empty otherwise
     */
    public Optional<ClassificationResult> classify(ClassificationTask task) {
        return classify(task.source(), task.target());
    }

    /**
     * Classifies a collection of classification tasks.
     * This method can process the classification either sequentially or in parallel
     * depending on the number of threads configured.
     *
     * @param classificationTasks The collection of classification tasks to classify
     * @return A list of classification results
     */
    public List<ClassificationResult> classify(Collection<ClassificationTask> classificationTasks) {
        return classify(createClassificationTasks(classificationTasks));
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
    public abstract Classifier copyOf();

    /**
     * Sets the prompt used for classification.
     * This method is only intended for use by optimizers.
     *
     * @param prompt The prompt template to use for classification
     */
    public abstract void setClassificationPrompt(String prompt);

    /**
     * Creates a list of classification tasks from source and target element stores.
     * Each task represents a pair of elements to be classified.
     *
     * @param sourceStore The store containing source elements
     * @param targetStore The store containing target elements
     * @return A list of element pairs to classify
     */
    protected static List<Pair<Element, Element>> createClassificationTasks(
            SourceElementStore sourceStore, TargetElementStore targetStore) {
        List<Pair<Element, Element>> tasks = new ArrayList<>();

        for (var source : sourceStore.getAllElements(true)) {
            var targetCandidates = targetStore.findSimilar(source);
            for (Element target : targetCandidates) {
                tasks.add(new Pair<>(source.first(), target));
            }
        }
        return tasks;
    }

    private static List<Pair<Element, Element>> createClassificationTasks(
            Collection<ClassificationTask> classificationTasks) {
        List<Pair<Element, Element>> tasks = new ArrayList<>();
        for (ClassificationTask task : classificationTasks) {
            tasks.add(new Pair<>(task.source(), task.target()));
        }
        return tasks;
    }

    /**
     * Creates a classifier instance based on the provided configuration.
     * The type of classifier is determined by the first part of the configuration name.
     *
     * @param configuration The module configuration for the classifier
     * @param contextStore The shared context store for pipeline components
     * @return A new classifier instance
     * @throws IllegalStateException If the configuration name is not recognized
     */
    public static Classifier createClassifier(ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case MOCK_CLASSIFIER_NAME -> new MockClassifier(contextStore);
            case SIMPLE_CLASSIFIER_NAME -> new SimpleClassifier(configuration, contextStore);
            case REASONING_CLASSIFIER_NAME -> new ReasoningClassifier(configuration, contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    public static String createClassificationPromptKey(ModuleConfiguration configuration) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case MOCK_CLASSIFIER_NAME ->
                throw new UnsupportedOperationException(
                        "MockClassifier does not support retrieving a single classification prompt key.");
            case SIMPLE_CLASSIFIER_NAME -> SimpleClassifier.getClassificationPromptKey();
            case REASONING_CLASSIFIER_NAME -> ReasoningClassifier.getClassificationPromptKey();
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    /**
     * Creates a multi-stage classifier that processes elements through a pipeline of classifiers.
     * Each stage in the pipeline can have multiple configurations that are processed in sequence.
     *
     * @param configs A list of configuration lists, where each inner list represents a stage
     * @param contextStore The shared context store for pipeline components
     * @return A new pipeline classifier instance
     */
    public static Classifier createMultiStageClassifier(
            List<List<ModuleConfiguration>> configs, ContextStore contextStore) {
        return new PipelineClassifier(configs, contextStore);
    }
}
