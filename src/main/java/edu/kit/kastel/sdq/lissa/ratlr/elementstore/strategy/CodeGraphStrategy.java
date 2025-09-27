package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.List;

public enum CodeGraphStrategy implements RetrievalStrategy {
    SINGLE_COMPONENT((query, components) -> null);
    
    private final RetrievalStrategy strategy;

    CodeGraphStrategy(RetrievalStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public List<Pair<Element, Float>> findSimilarElements(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        return strategy.findSimilarElements(query, allElementsInStore);
    }
}
