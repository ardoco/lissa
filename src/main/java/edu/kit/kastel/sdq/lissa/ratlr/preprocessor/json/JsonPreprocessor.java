package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

import java.util.ArrayList;
import java.util.List;

public abstract class JsonPreprocessor extends Preprocessor<Element> {
    
    protected static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore        The shared context store for pipeline components
     */
    protected JsonPreprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public final List<Element> preprocess(List<Element> elements) {
        List<Element> result = new ArrayList<>();
        for (Element element : elements) {
            try {
                preprocess(element, result);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    protected abstract void preprocess(Element element, List<Element> elements) throws JsonProcessingException;
}
