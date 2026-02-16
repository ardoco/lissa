/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.scorer;

import java.util.List;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;

/**
 * Interface for scoring classification results against classification tasks.
 * Implementations of this interface provide methods to evaluate the quality
 * of classification results based on the ground truth provided in the tasks.
 */
public interface Scorer {

    /**
     * Scores a list of classification tasks against their corresponding classification results.
     * Each task is scored based on whether a result is provided or null.
     *
     * @param tasks   The list of classification tasks to be scored
     * @param results The list of classification results corresponding to the tasks. May contain null values
     * @return A list of scores for each classification task
     * @throws IllegalArgumentException if the sizes of tasks and results lists do not match
     */
    List<Double> score(List<ClassificationTask> tasks, List<ClassificationResult> results);

    /**
     * Scores a classification result against a classification task.
     *
     * @param task   The classification task containing the ground truth
     * @param result The classification result to be scored
     * @return A double score representing the quality of the classification result
     */
    double score(ClassificationTask task, @Nullable ClassificationResult result);

    /**
     * Scores a classification task when no classification result was produced.
     * This is called when the classifier did not identify a trace link for this task (i.e., the classifier rejected it).
     * The score should reflect whether this rejection was correct based on the task's ground truth label.
     *
     * @param task The classification task containing the ground truth
     * @return A score representing how correct the absence of a classification result is for this task
     */
    double score(ClassificationTask task);

    /**
     * Returns the name of the metric.
     *
     * @return The name of the metric
     */
    String getName();
}
