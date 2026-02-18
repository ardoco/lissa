/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric;

import java.util.Set;

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.postprocessor.TraceLinkIdPostprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.resultaggregator.ResultAggregator;

/**
 * A metric that calculates the F-beta score for binary classification tasks.
 * The F-beta score is a weighted harmonic mean of precision and recall, where beta
 * determines the weight of recall in the combined score.
 *
 * @see <a href="https://en.wikipedia.org/wiki/F-score">F-Score</a>
 */
public class FBetaMetric extends GlobalMetric {

    /**
     * The beta parameter for the F-beta score calculation.
     */
    private static final int DEFAULT_BETA = 1;

    private static final String BETA_CONFIGURATION_KEY = "beta";
    private final int beta;

    /**
     * Creates a new binary metric instance with the given configuration.
     *
     * @param configuration The configuration for the metric.
     * @param classifier    The classifier to use for scoring.
     */
    public FBetaMetric(
            ModuleConfiguration configuration,
            Classifier classifier,
            ResultAggregator aggregator,
            TraceLinkIdPostprocessor postprocessor) {
        super(classifier, aggregator, postprocessor);
        this.beta = configuration.argumentAsInt(BETA_CONFIGURATION_KEY, DEFAULT_BETA);
    }

    /**
     * Reduces the given collections of items, rejected items, and ground truth into a single F-beta score.
     */
    @Override
    public <T> double reduce(Set<T> items, Set<T> groundTruth) {
        ClassificationMetricsCalculator cmc = ClassificationMetricsCalculator.getInstance();
        var classification = cmc.calculateMetrics(items, groundTruth, null);

        return classification.fBeta(beta);
    }

    @Override
    public String getName() {
        return "F%s-Score".formatted(beta);
    }
}
