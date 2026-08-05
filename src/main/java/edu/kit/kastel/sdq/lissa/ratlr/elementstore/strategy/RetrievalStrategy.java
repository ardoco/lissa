/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * Interface defining the contract for retrieval strategies used to find similar elements based on their vector representations.
 */
public interface RetrievalStrategy {
    Logger logger = LoggerFactory.getLogger(RetrievalStrategy.class);

    /**
     * Finds similar elements to the given query element based on their vector representations.
     *
     * @param query The query element and its vector representation
     * @param allElementsInStore A list of all elements in the store along with their vector representations
     * @return A list of pairs containing similar elements and their similarity scores, sorted by similarity in descending order
     */
    List<Pair<Element, Float>> findSimilarElements(
            Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore);

    /**
     * Factory method to create an instance of RetrievalStrategy based on the provided configuration.
     * This method uses the configuration name to determine which specific retrieval strategy implementation to instantiate.
     *
     * @param configuration The configuration for the retrieval strategy
     * @return An instance of RetrievalStrategy based on the configuration
     */
    static RetrievalStrategy createStrategy(ModuleConfiguration configuration) {
        return switch (configuration.name()) {
            case "cosine_similarity" -> new CosineSimilarity(configuration);
            case "custom" -> {
                logger.warn("For backwards compatibility: Using cosine similarity as default retrieval strategy.");
                yield new CosineSimilarity(configuration);
            }
            default -> throw new IllegalStateException("Unknown strategy name: " + configuration.name());
        };
    }
}
