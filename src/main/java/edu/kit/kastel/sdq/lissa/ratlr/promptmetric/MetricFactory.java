/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;

/**
 * Factory class for creating metric instances based on the provided configuration.
 */
public final class MetricFactory {

    private MetricFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

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
    public static Metric createScorer(
            ModuleConfiguration configuration,
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
