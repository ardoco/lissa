/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import java.util.Collection;

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
    public <T> double reduce(Collection<T> items, Collection<T> rejectedItems, Collection<T> groundTruth) {
        int truePositive = 0;
        int falsePositive = 0;
        int falseNegative = 0;
        int trueNegative = 0;

        for (T item : items) {
            if (groundTruth.contains(item)) {
                truePositive++;
            } else {
                falsePositive++;
            }
        }
        for (T item : rejectedItems) {
            if (groundTruth.contains(item)) {
                falseNegative++;
            } else {
                trueNegative++;
            }
        }

        if (truePositive + trueNegative == 0) {
            return 0.0;
        }
        if (falsePositive + falseNegative == 0) {
            return 1.0;
        }

        return fBeta(truePositive, falsePositive, falseNegative, beta);
    }

    @Override
    public String getName() {
        return "F%s-Score".formatted(beta);
    }

    private static double recall(int truePositive, int falseNegative) {
        return (double) truePositive / (truePositive + falseNegative);
    }

    private static double precision(int truePositive, int falsePositive) {
        return (double) truePositive / (truePositive + falsePositive);
    }

    private static double fBeta(double precision, double recall, int beta) {
        return ((1 + beta * beta) * precision * recall) / ((beta * beta * precision) + recall);
    }

    private static double fBeta(int truePositive, int falsePositive, int falseNegative, int beta) {
        double precision = precision(truePositive, falsePositive);
        double recall = recall(truePositive, falseNegative);
        return fBeta(precision, recall, beta);
    }
}
