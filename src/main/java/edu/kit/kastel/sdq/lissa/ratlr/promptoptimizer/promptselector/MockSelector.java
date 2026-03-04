/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;

/**
 * A mock evaluator that returns a fixed score for all prompts, regardless of the input.
 * This is useful for testing and debugging the optimization pipeline without relying on actual evaluation logic.
 */
public class MockSelector implements Selector {

    /**
     * Creates a new mock evaluator instance with a default configuration.
     * The configuration is not used in this evaluator, as it returns fixed scores.
     */
    public MockSelector() {}

    /**
     * Dummy implementation that returns a fixed score of 1.0 for all prompts, regardless of the input classification tasks or metric.
     */
    @Override
    public List<Double> selectAndEvaluate(
            List<String> prompts, @Nullable List<ClassificationTask> examples, @Nullable Metric metric) {
        return Collections.nCopies(prompts.size(), 1.0);
    }
}
