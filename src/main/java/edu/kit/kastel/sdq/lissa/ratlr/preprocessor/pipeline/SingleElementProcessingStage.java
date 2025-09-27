package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.ArrayList;
import java.util.List;

public abstract class SingleElementProcessingStage extends PipelineStage {

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected SingleElementProcessingStage(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public List<Element> process(List<Element> elements) {
        List<Element> result = new ArrayList<>(elements.size());
        for (Element element : elements) {
            result.addAll(process(element));
        }
        return result;
    }
    
    protected abstract List<Element> process(Element element);
}
