package edu.kit.kastel.sdq.lissa.ratlr.context;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementRetrieval implements Context {

    public static final String IDENTIFIER = ElementRetrieval.class.getName();
    private final Map<Element, List<Element>> retrievalStrategyByQuery = new HashMap<>();
    
    public void setRetrieval(Element element, List<Element> correspondingElements) {
        retrievalStrategyByQuery.put(element, correspondingElements);
    }
    
    public List<Element> retrieve(Element element) {
        return retrievalStrategyByQuery.get(element);
    }
    
    @Override
    public String getId() {
        return IDENTIFIER;
    }
}
