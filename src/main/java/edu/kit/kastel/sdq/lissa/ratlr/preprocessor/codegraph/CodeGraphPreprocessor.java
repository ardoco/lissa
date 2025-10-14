package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

public abstract class CodeGraphPreprocessor extends Preprocessor {
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected CodeGraphPreprocessor(ContextStore contextStore) {
        super(contextStore);
        if (!contextStore.hasContext(CodeGraph.CONTEXT_IDENTIFIER)) {
            throw new IllegalStateException("illegal artifact provider, must be 'code'");
        }
    }
}
