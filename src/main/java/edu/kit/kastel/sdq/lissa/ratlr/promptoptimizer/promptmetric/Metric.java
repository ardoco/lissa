/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric;

import java.util.List;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;

/**
 * The Metric interface defines methods for evaluating prompts based on classification tasks.
 * Implementations of this interface should provide mechanisms to compute metrics for
 * single prompts as well as batches of prompts.
 * <br>
 * The computed metrics should be in the range [{@value #MINIMUM_SCORE}, {@value #MAXIMUM_SCORE}],
 * where higher values indicate better performance.
 */
public interface Metric {

    /**
     * The maximum score a metric can return.
     */
    double MAXIMUM_SCORE = 1.0;

    /**
     * The minimum score a metric can return.
     */
    double MINIMUM_SCORE = 0.0;

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

    /**
     * Factory method to create a metric based on the provided configuration.
     * The name field indicates the type of metric to create.
     * If the configuration is null, a MockMetric is returned by default.
     *
     * @param configuration The configuration specifying the type of metric to create.
     * @param classifier The classifier to be used by the metric.
     * @return An instance of a concrete metric implementation.
     * @throws IllegalStateException If the configuration name does not match any known metric types.
     */
    static Metric createMetric(
            @Nullable ModuleConfiguration configuration,
            Classifier classifier,
            ResultAggregator aggregator,
            TraceLinkIdPostprocessor postprocessor) {
        if (configuration == null) {
            return new MockMetric();
        }
        return switch (configuration.name()) {
            case "mock" -> new MockMetric();
            case "pointwise" -> new PointwiseMetric(configuration, classifier);
            case "fBeta", "f1" -> new FBetaMetric(configuration, classifier, aggregator, postprocessor);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
