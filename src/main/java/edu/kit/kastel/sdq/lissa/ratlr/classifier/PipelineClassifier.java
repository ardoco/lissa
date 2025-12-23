/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A classifier that processes elements through a pipeline of multiple classifier stages.
 * Each stage consists of one or more classifiers that vote on whether elements are related.
 * Elements must receive a majority vote from the classifiers in a stage to proceed to the next stage.
 * This approach allows for more robust classification by combining multiple classification strategies.
 * <p>
 * The pipeline classifier and its stages can access shared context via a {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore},
 * which is passed to each classifier during instantiation.
 * </p>
 */
public class PipelineClassifier extends Classifier {
    /**
     * The list of classifier stages, where each stage is a list of classifiers.
     */
    private final List<List<Classifier>> classifiers;

    /**
     * Creates a new pipeline classifier from a list of configuration lists.
     * Each inner list represents a stage in the pipeline, and each configuration
     * in a stage is used to create a classifier. All classifiers receive the shared {@link ContextStore}.
     *
     * @param configs A list of configuration lists, where each inner list represents a stage
     * @param contextStore The shared context store for pipeline components
     */
    public PipelineClassifier(List<List<ModuleConfiguration>> configs, ContextStore contextStore) {
        super(1, contextStore);
        this.classifiers = configs.stream()
                .map(it -> it.stream()
                        .map(config -> Classifier.createClassifier(config, contextStore))
                        .toList())
                .toList();
    }

    /**
     * Creates a new pipeline classifier with the specified classifiers and thread count.
     * This constructor is used internally for creating thread-local copies.
     *
     * @param classifiers The list of classifier stages
     * @param threads The number of threads to use for parallel processing
     */
    private PipelineClassifier(List<List<Classifier>> classifiers, int threads, ContextStore contextStore) {
        super(threads, contextStore);
        this.classifiers = classifiers.stream().map(List::copyOf).toList();
    }

    /**
     * Classifies elements through the pipeline of classifier stages.
     * Each stage reduces the number of potential trace links based on majority voting
     * among its classifiers. The process continues until either all stages are processed
     * or no trace links remain.
     *
     * @param sourceStore The store containing source elements
     * @param targetStore The store containing target elements
     * @return A list of classification results for the remaining trace links
     */
    @Override
    public List<ClassificationResult> classify(SourceElementStore sourceStore, TargetElementStore targetStore) {
        List<ClassificationResult> results = new ArrayList<>();
        List<Pair<Element, Element>> tasks = createClassificationTasks(sourceStore, targetStore);

        int layerNum = 0;
        for (List<Classifier> layer : classifiers) {
            logger.info("Invoking layer {} with {} classifiers and {} tasks", layerNum, layer.size(), tasks.size());
            layerNum++;

            List<Pair<Element, Element>> layerResults = calculateRemainingCandidates(tasks, layer);
            logger.info("Reduced targets @ layer {} from {} to {}", layerNum, tasks.size(), layerResults.size());

            tasks = layerResults;
            if (tasks.isEmpty()) {
                logger.info("No remaining targets after layer {}, stopping classification.", layerNum);
                break;
            }
        }

        for (Pair<Element, Element> sourceXtarget : tasks) {
            ClassificationResult result = ClassificationResult.of(sourceXtarget.first(), sourceXtarget.second(), 1.0);
            results.add(result);
        }

        return results;
    }

    /**
     * Calculates the remaining candidates after processing through a stage of classifiers.
     * Each classifier in the stage votes on whether elements are related, and elements
     * must receive a majority vote to proceed to the next stage.
     *
     * @param tasks The list of element pairs to classify
     * @param classifiers The list of classifiers in the current stage
     * @return A list of element pairs that received a majority vote
     */
    private List<Pair<Element, Element>> calculateRemainingCandidates(
            List<Pair<Element, Element>> tasks, List<Classifier> classifiers) {
        Map<Pair<Element, Element>, AtomicInteger> counter = new LinkedHashMap<>();
        for (var task : tasks) {
            counter.put(task, new AtomicInteger(0));
        }

        for (Classifier classifier : classifiers) {
            if (tasks.isEmpty()) break;
            List<ClassificationResult> classificationResults;
            if (classifier.threads <= 1) {
                classificationResults = tasks.stream()
                        .map(e -> classifier.classify(e.first(), e.second()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();
            } else {
                classificationResults = classifier.parallelClassify(tasks).stream()
                        .filter(Objects::nonNull)
                        .toList();
            }
            for (var result : classificationResults) {
                counter.get(new Pair<>(result.source(), result.target())).incrementAndGet();
            }
        }

        List<Pair<Element, Element>> remainingTargetsAfterMajorityVote = new ArrayList<>();
        int majorityThreshold = (int) Math.ceil(classifiers.size() / 2.0);
        for (var entry : counter.entrySet()) {
            if (entry.getValue().get() >= majorityThreshold) {
                remainingTargetsAfterMajorityVote.add(entry.getKey());
            }
        }

        return remainingTargetsAfterMajorityVote;
    }

    @Override
    public Classifier copyOf() {
        return new PipelineClassifier(classifiers, this.threads, this.contextStore);
    }

    @Override
    public void setClassificationPrompt(String prompt) {
        throw new UnsupportedOperationException(
                "PipelineClassifiers do not support setting a single classification prompt. Configure individual classifiers instead.");
    }

    @Override
    public String getClassificationPromptKey() {
        throw new UnsupportedOperationException(
                "PipelineClassifiers do not support retrieving a single classification prompt key. Access individual classifiers instead.");
    }

    /**
     * This method is not supported by the pipeline classifier.
     * The pipeline classifier processes elements through multiple stages and cannot
     * classify single pairs directly.
     *
     * @throws UnsupportedOperationException Always thrown, as this operation is not supported
     */
    @Override
    protected Optional<ClassificationResult> classify(Element source, Element targets) {
        throw new UnsupportedOperationException("PipelineClassifier does not support single pair classification.");
    }
}
