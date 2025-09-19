package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.LinkedList;
import java.util.List;

public class IndependentParallel extends Preprocessor<Element> {

    private final List<Preprocessor<Element>> preprocessors;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected IndependentParallel(ContextStore contextStore) {
        super(contextStore);
        this.preprocessors = List.of();
    }

    @Override
    public List<Element> preprocess(List<Element> artifacts) {
        List<Element> elements = new LinkedList<>();
        for (Preprocessor<Element> preprocessor : preprocessors) {
            elements.addAll(preprocessor.preprocess(artifacts));
        }
        return elements;
    }
}
