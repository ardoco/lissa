package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.List;

public class DocumentationSectionSplitter extends SingleElementProcessingStage {
    
    
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected DocumentationSectionSplitter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    protected List<Element> process(Element element) {
        return List.of();
    }
}
