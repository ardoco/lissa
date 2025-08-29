package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.ArrayList;
import java.util.List;

public abstract class SingleElementPreprocessor extends Preprocessor<Element> {
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected SingleElementPreprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public List<Element> preprocess(List<Element> elements) {
        List<Element> result = new ArrayList<>(elements.size());
        for (Element element : elements) {
            result.addAll(preprocess(element));
        }
        return result;
    }
    
    protected abstract List<Element> preprocess(Element element);
}
