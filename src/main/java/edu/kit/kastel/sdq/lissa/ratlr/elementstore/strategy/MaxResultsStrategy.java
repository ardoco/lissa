package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.List;

public abstract class MaxResultsStrategy implements RetrievalStrategy {

    /**
     * Special value for the maximum number of results that indicates no limit.
     * Only applicable for target stores (similarityRetriever = true) in LiSSA's similarity search.
     */
    private static final String MAX_RESULTS_INFINITY_ARGUMENT = "infinity";
    protected final int maxResults;

    protected MaxResultsStrategy(ModuleConfiguration configuration) {
        final String maxResultsKey = "max_results";
        boolean isInfinity = configuration.hasArgument(maxResultsKey)
                && configuration.argumentAsString(maxResultsKey).equalsIgnoreCase(MAX_RESULTS_INFINITY_ARGUMENT);

        if (isInfinity) {
            this.maxResults = Integer.MAX_VALUE;
        } else {
            this.maxResults = configuration.argumentAsInt(maxResultsKey, 10);
            if (maxResults < 1) {
                throw new IllegalArgumentException("The maximum number of results must be greater than 0.");
            }
        }
    }

    @Override
    public final List<Pair<Element, Float>> findSimilarElements(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        List<Pair<Element, Float>> results = findSimilarElementsInternal(query, allElementsInStore);
        results.sort((a, b) -> Float.compare(b.second(), a.second()));
        return results.subList(0, Math.min(maxResults, results.size()));
    }
    
    protected abstract List<Pair<Element, Float>> findSimilarElementsInternal(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore);
}
