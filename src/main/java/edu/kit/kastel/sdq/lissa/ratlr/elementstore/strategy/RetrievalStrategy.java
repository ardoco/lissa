/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

public interface RetrievalStrategy {
    Logger logger = LoggerFactory.getLogger(RetrievalStrategy.class);

    List<Pair<Element, Float>> findSimilarElements(
            Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore);

    static RetrievalStrategy createStrategy(ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name()) {
            case "cosine_similarity" -> new CosineSimilarity(configuration);
            case "token_similarity" -> new TokenSimilarity(configuration);
            case "cosine_token_similarity" -> new CosineTokenSimilarity(configuration);
            case "occurrence_similarity" -> new OccurrenceSimilarity(configuration);
            case "code_graph_strategy" -> new CodeGraphStrategy(contextStore);
            case "custom" -> {
                logger.warn("For backwards compatibility: Using cosine similarity as default retrieval strategy.");
                yield new CosineSimilarity(configuration);
            }
            default -> throw new IllegalStateException("Unknown strategy name: " + configuration.name());
        };
    }
}
