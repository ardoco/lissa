package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.nl;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TextSplitter extends Preprocessor<Element> {
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public TextSplitter(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public List<Element> preprocess(List<Element> elements) {
        List<Element> result = new LinkedList<>();
        for (Element element : elements) {
            result.addAll(preprocess(element));
        }
        return result;
    }

    private Collection<Element> preprocess(Element element) {
        return null;
    }
}
