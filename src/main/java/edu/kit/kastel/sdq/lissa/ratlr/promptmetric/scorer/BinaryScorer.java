/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.scorer;

import static edu.kit.kastel.sdq.lissa.ratlr.promptmetric.MetricUtils.MAXIMUM_SCORE;
import static edu.kit.kastel.sdq.lissa.ratlr.promptmetric.MetricUtils.MINIMUM_SCORE;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;

/**
 * This metric assigns a fixed score for correct and incorrect classifications.
 * The presence of a classification result indicates a correct classification.
 * If no result is provided, the classification is considered incorrect.
 */
public class BinaryScorer implements Scorer {

    private final double correctClassificationScore;
    private final double incorrectClassificationScore;

    public BinaryScorer() {
        this.correctClassificationScore = MAXIMUM_SCORE;
        this.incorrectClassificationScore = MINIMUM_SCORE;
    }

    /**
     * Scores a list of classification tasks against their corresponding classification results.
     * Depending on the presence of a result, each task is scored accordingly with the {@link #score(ClassificationTask, ClassificationResult)}
     * or {@link #score(ClassificationTask)} methods.
     *
     * @param tasks   The list of classification tasks to be scored
     * @param results The list of classification results corresponding to the tasks. May contain null values
     * @return A list of scores for each classification task
     * @throws IllegalArgumentException if the sizes of tasks and results lists do not match
     */
    @Override
    public List<Double> score(List<ClassificationTask> tasks, List<ClassificationResult> results) {
        if (tasks.size() != results.size()) {
            throw new IllegalArgumentException("Tasks and results lists must have the same size.");
        }
        List<Double> scores = new ArrayList<>(tasks.size());
        for (int i = 0; i < tasks.size(); i++) {
            scores.add(score(tasks.get(i), results.get(i)));
        }
        return scores;
    }

    /**
     * Scores the classification task based on the presence of a result. If a result is provided, not null and the task
     * is labeled as true, it is considered a correct classification and receives the correctClassificationScore.
     *
     * @param task   The classification task to be scored
     * @param result The classification result, which may be null
     * @return       The score for the classification task
     */
    @Override
    public double score(ClassificationTask task, ClassificationResult result) {
        if (result == null) {
            return score(task);
        }
        return task.label() ? correctClassificationScore : incorrectClassificationScore;
    }

    /**
     * Scores the classification task when no result is provided. If the task is labeled as false, it is considered a
     * correct classification and receives the correctClassificationScore.
     *
     * @param task The classification task to be scored
     * @return     The score for the classification task
     */
    @Override
    public double score(ClassificationTask task) {
        return task.label() ? incorrectClassificationScore : correctClassificationScore;
    }

    @Override
    public String getName() {
        return "binary";
    }
}
