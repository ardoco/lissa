package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CosineTokenSimilarity extends MaxResultsStrategy {
    
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CosineSimilarity cosineSimilarity;
    private final TokenSimilarity tokenSimilarity;

    public CosineTokenSimilarity(ModuleConfiguration configuration) {
        super(configuration);
        this.cosineSimilarity = new CosineSimilarity(configuration);
        this.tokenSimilarity = new TokenSimilarity(configuration);
    }

    @Override
    public List<Pair<Element, Float>> findSimilarElementsInternal(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        logger.info("receiving {} elements in store", allElementsInStore.size());
        List<Pair<Element, Float>> cosineSimilarity = this.cosineSimilarity.findSimilarElementsInternal(query, allElementsInStore);
        Map<Element, Float> cosineMapping = new HashMap<>();
        for (Pair<Element, Float> pair : cosineSimilarity) {
            cosineMapping.put(pair.first(), pair.second());
        }
        List<Pair<Element, Float>> tokenSimilarity = this.tokenSimilarity.findSimilarElementsInternal(query, allElementsInStore);
        if (tokenSimilarity.size() != cosineSimilarity.size()) {
            throw new IllegalStateException("tokenSimilarity.size() != cosineSimilarity.size()");
        }
        List<Pair<Element, Float>> results = new ArrayList<>(tokenSimilarity.size());
        for (Pair<Element, Float> pair : tokenSimilarity) {
            results.add(new Pair<>(pair.first(), pair.second() * cosineMapping.get(pair.first())));
        }
        return results;
    }
}
