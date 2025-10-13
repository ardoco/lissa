package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.List;

public class CodeRelationPreprocessor extends CodeGraphPreprocessor {
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected CodeRelationPreprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        return List.of();
    }
}
