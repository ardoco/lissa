/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;

/**
 * The Metric interface defines methods for evaluating prompts based on classification tasks.
 * Implementations of this interface should provide mechanisms to compute metrics for
 * single prompts as well as batches of prompts.
 * <br>
 * The computed metrics should be in the range [{@value MetricUtils#MINIMUM_SCORE}, {@value MetricUtils#MAXIMUM_SCORE}],
 * where higher values indicate better performance.
 */
public interface Metric {

    /**
     * Computes metrics for a list of prompts against a list of classification tasks.
     * Each prompt is evaluated against all provided classification tasks.
     * The result is a list of metric values, one for each prompt.
     *
     * @param prompts  The list of prompts to be evaluated
     * @param examples The list of classification tasks to evaluate against
     * @return A list of metric values corresponding to each prompt
     */
    List<Double> getMetric(List<String> prompts, List<ClassificationTask> examples);

    /**
     * Computes the metric for a single prompt against a list of classification tasks.
     * The result is a single metric value representing the performance of the prompt.
     *
     * @param prompt   The prompt to be evaluated
     * @param examples The list of classification tasks to evaluate against
     * @return A double value representing the metric for the given prompt
     */
    Double getMetric(String prompt, List<ClassificationTask> examples);

    /**
     * Returns the name of the metric.
     *
     * @return The name of the metric
     */
    String getName();
}
