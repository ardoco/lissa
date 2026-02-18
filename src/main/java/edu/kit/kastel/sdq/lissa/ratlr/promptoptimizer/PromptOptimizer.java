/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration.CONFIG_NAME_SEPARATOR;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.evaluator.Evaluator;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;

/**
 * Interface for prompt optimizers in the LiSSA framework.
 * This class provides the foundation for implementing different prompt optimization strategies
 * for trace link analysis.
 */
public interface PromptOptimizer {

    /**
     * Runs the optimization process.
     * This method should be implemented to define the specific optimization logic.
     *
     * @param sourceStore The store containing source elements of the domain/dataset the prompt is optimized for
     * @param targetStore The store containing target elements of the domain/dataset the prompt is optimized for
     * @return A string representing the optimized prompt
     */
    String[] optimize(SourceElementStore sourceStore, TargetElementStore targetStore);

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
    static PromptOptimizer createOptimizer(
            @Nullable ModuleConfiguration configuration, Set<TraceLink> goldStandard, Metric metric, Evaluator evaluator) {
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
