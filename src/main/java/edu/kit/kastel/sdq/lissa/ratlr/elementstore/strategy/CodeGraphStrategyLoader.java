package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ElementRetrieval;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGraphStrategyLoader implements RetrievalStrategy {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ContextStore contextStore;
    private final RetrievalStrategy cosineSimilarity;

    public CodeGraphStrategyLoader(ModuleConfiguration configuration, ContextStore contextStore) {
        this.contextStore = contextStore;
        if (!contextStore.hasContext(ElementRetrieval.IDENTIFIER)) {
            throw new IllegalStateException("illegal artifact provider, must be 'code'");
        }
        Map<String, String> baseSimilarityArguments = new HashMap<>();
        for (String argumentKey : configuration.argumentKeys()) {
            if (!argumentKey.equals("name")) {
                baseSimilarityArguments.put(argumentKey, configuration.argumentAsString(argumentKey));
            }
        }
        this.cosineSimilarity = RetrievalStrategy.createStrategy(new ModuleConfiguration(configuration.argumentAsString("name"), baseSimilarityArguments), contextStore);
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
        List<Pair<Element, Float>> similarityResult = cosineSimilarity.findSimilarElements(query, components);
        for (Pair<Element, Float> pair : similarityResult) {
            logger.info("similarity for {} and {}: {}", query.first().getIdentifier(), pair.first().getIdentifier(), pair.second());
        }
        List<Element> elementsToBeRetrieved = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class).retrieve(similarityResult.getFirst().first());
        if (elementsToBeRetrieved == null) {
            return List.of();
        }
        
        List<Pair<Element, Float>> retrievedElements = new ArrayList<>(elementsToBeRetrieved.size());
        for (Element element : elementsToBeRetrieved) {
            retrievedElements.add(new Pair<>(element, similarityResult.getFirst().second()));
        }
        return retrievedElements;
    }
    
}
