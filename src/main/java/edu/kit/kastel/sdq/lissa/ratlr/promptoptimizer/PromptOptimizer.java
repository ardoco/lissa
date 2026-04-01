/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration.CONFIG_NAME_SEPARATOR;

import java.util.List;
import java.util.Set;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector.Selector;

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
     * @return A list of Strings representing the optimized prompts. The last entry in the list is the final
     * optimized prompt, while the preceding entries represent intermediate prompts during the optimization
     * process. If no optimization is performed (i.e. config maximum_iterations = 0), the list is empty.
     */
    List<String> optimize(SourceElementStore sourceStore, TargetElementStore targetStore);

    /**
     * Factory method to create an instance of PromptOptimizer based on the provided configuration.
     * This method uses the configuration name to determine which specific optimizer implementation to instantiate.
     *
     * @param configuration The configuration for the optimizer
     * @param goldStandard The gold standard trace links for evaluation
     * @param metric The metric used to evaluate the prompt performance
     * @param selector The selector used to assess the optimization results
     * @return An instance of PromptOptimizer based on the configuration
     */
    static PromptOptimizer createOptimizer(
            ModuleConfiguration configuration, Set<TraceLink> goldStandard, Metric metric, Selector selector) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case "mock" -> new MockOptimizer();
            case "simple" -> new IterativeOptimizer(configuration, goldStandard, metric, 1);
            case "iterative" -> new IterativeOptimizer(configuration, goldStandard, metric);
            case "feedback" -> new IterativeFeedbackOptimizer(configuration, goldStandard, metric);
            case "gradient", "protegi" -> new ProTeGiOptimizer(configuration, goldStandard, metric, selector);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
