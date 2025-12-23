/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import java.util.Set;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.evaluator.Evaluator;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;

/**
 * Factory class for creating instances of PromptOptimizer based on the provided configuration.
 * This class uses the factory design pattern to encapsulate the instantiation logic for different
 * prompt optimizer implementations.
 */
public final class OptimizerFactory {

    /**
     * Separator used in configuration names.
     */
    public static final String CONFIG_NAME_SEPARATOR = "_";

    private OptimizerFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

    /**
     * Factory method to create an instance of PromptOptimizer based on the provided configuration.
     * This method uses the configuration name to determine which specific optimizer implementation to instantiate.
     *
     * @param configuration The configuration for the optimizer
     * @param goldStandard The gold standard trace links for evaluation
     * @param metric The metric used to evaluate the prompt performance
     * @param evaluator The evaluator used to assess the optimization results
     * @return An instance of PromptOptimizer based on the configuration
     */
    public static PromptOptimizer createOptimizer(
            ModuleConfiguration configuration, Set<TraceLink> goldStandard, Metric metric, Evaluator evaluator) {
        if (configuration == null) {
            return new MockOptimizer();
        }
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case "mock" -> new MockOptimizer();
            case "simple" -> new IterativeOptimizer(configuration, goldStandard, metric, 1);
            case "iterative" -> new IterativeOptimizer(configuration, goldStandard, metric);
            case "feedback" -> new IterativeFeedbackOptimizer(configuration, goldStandard, metric);
            case "gradient" -> new AutomaticPromptOptimizer(configuration, goldStandard, metric, evaluator);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
