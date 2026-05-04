/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;

/**
 * The Selector interface defines methods for selecting and evaluating prompts in LiSSA's prompt optimization module.
 * <p>
 * Selectors in the LiSSA framework are responsible for evaluating a set of prompts based on their performance on a
 * given set of classification tasks and a specified metric while respecting an evaluation budget.
 * <br>
 * Implementations of this interface should provide mechanisms to select which prompts to evaluate
 * and in what order, potentially using strategies such as random sampling, bandit algorithms, or exhaustive search.
 */
public interface Selector {

    /**
     * Default seed for random number generation to ensure reproducibility.
     */
    int DEFAULT_SEED = 42069;

    /**
     * Default number of samples to use for evaluating each prompt.
     */
    int SAMPLES_PER_EVAL = 32;

    /**
     * Evaluates a list of prompts using the provided classification tasks and metric.
     *
     * @param prompts A list of prompts to evaluate.
     * @param examples A list of classification task examples.
     * @param metric The metric instance to use for scoring the prompts.
     * @return A list of scores corresponding to each prompt.
     */
    List<Double> selectAndEvaluate(List<String> prompts, List<ClassificationTask> examples, Metric metric);

    /**
     * Factory method to create a selector based on the provided configuration.
     * The name field indicates the type of selector to create.
     *
     * @param configuration The configuration specifying the type of selector to create.
     * @return An instance of a concrete selector implementation.
     * @throws IllegalStateException If the configuration name does not match any known selector types.
     */
    static Selector createSelector(ModuleConfiguration configuration) {
        return switch (configuration.name()) {
            case "simple", "bruteforce" -> new SimpleSelector(configuration);
            case "ucb" -> new UpperConfidenceBoundBanditSelector(configuration);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
