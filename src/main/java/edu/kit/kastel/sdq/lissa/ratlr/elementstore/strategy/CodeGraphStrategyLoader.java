package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.context.ElementRetrieval;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class CodeGraphStrategyLoader implements RetrievalStrategy {

    private final ContextStore contextStore;
    private final RetrievalStrategy cosineSimilarity;

    public CodeGraphStrategyLoader(ContextStore contextStore) {
        this.contextStore = contextStore;
        if (!contextStore.hasContext(ElementRetrieval.IDENTIFIER)) {
            throw new IllegalStateException("illegal artifact provider, must be 'code'");
        }
        this.cosineSimilarity = new CosineSimilarity(1);
    }
    
    @Override
    public List<Pair<Element, Float>> findSimilarElements(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        return findSingleComponent(query, allElementsInStore);
    }

    /**
     * Finds the component element with the highest cosine similarity and retrieves all contained artifact elements.
     *
     * @param query        the text element containing a single reference of the target component
     * @param components   the component elements registered in the {@link ElementRetrieval} context
     * @return all artifact elements contained in the found component
     */
    public List<Pair<Element, Float>> findSingleComponent(Pair<Element, float[]> query, List<Pair<Element, float[]>> components) {
        Pair<Element, Float> similarityResult = cosineSimilarity.findSimilarElements(query, components).getFirst();
        List<Element> elementsToBeRetrieved = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class).retrieve(similarityResult.first());
        List<Pair<Element, Float>> retrievedElements = new ArrayList<>(elementsToBeRetrieved.size());
        for (Element element : elementsToBeRetrieved) {
            retrievedElements.add(new Pair<>(element, similarityResult.second()));
        }
        return retrievedElements;
    }
    
}
