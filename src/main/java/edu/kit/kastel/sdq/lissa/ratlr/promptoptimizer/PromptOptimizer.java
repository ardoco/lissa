/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;

/**
 * Abstract base class for prompt optimizers in the LiSSA framework.
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
    String optimize(SourceElementStore sourceStore, TargetElementStore targetStore);
}
