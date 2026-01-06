/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;

/**
 * A mock implementation of the PromptOptimizer interface for testing purposes.
 * This optimizer does not perform any actual optimization and returns an empty string.
 */
public class MockOptimizer implements PromptOptimizer {

    public MockOptimizer() {
        // No specific initialization required
    }

    @Override
    public String optimize(SourceElementStore sourceStore, TargetElementStore targetStore) {
        return "";
    }
}
