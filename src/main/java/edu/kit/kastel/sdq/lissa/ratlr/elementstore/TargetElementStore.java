/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.RetrievalStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A store for target elements and their embeddings in the LiSSA framework.
 * Providing functionality for similarity search and element retrieval
 * <b>Target Store</b> (similarityRetriever = true):
 * <ul>
 *      <li>Used to store target elements that will be searched for similarity in LiSSA's classification phase</li>
 *      <li>Cannot retrieve all elements at once</li>
 * </ul>
 */
public class TargetElementStore extends ElementStore {

    /**
     * Strategy to find similar elements.
     */
    private final RetrievalStrategy retrievalStrategy;

    public TargetElementStore(ModuleConfiguration moduleConfiguration) {
        super();
        this.retrievalStrategy = RetrievalStrategy.createStrategy(moduleConfiguration);
    }

    public TargetElementStore(List<Pair<Element, float[]>> content, RetrievalStrategy retrievalStrategy) {
        super(content);
        this.retrievalStrategy = retrievalStrategy;
    }

    /**
     * Retrieves the retrieval strategy used for finding similar elements.
     *
     * @return The retrieval strategy
     */
    public RetrievalStrategy getRetrievalStrategy() {
        return retrievalStrategy;
    }

    /**
     * Retrieves all elements in the store without their embeddings.
     * This method is primarily used for batch processing operations that only need element metadata,
     * such as reducing the target store scope.
     *
     * @return List of all elements (without embeddings)
     */
    public List<Element> getAllElements() {
        return getAllElementsIntern(false).stream().map(Pair::first).toList();
    }

    /**
     * Finds elements similar to the given query vector as part of LiSSA's similarity matching.
     *
     * @param query The element and vector to find similar elements for
     * @return List of similar elements, sorted by similarity
     */
    public final List<Element> findSimilar(Pair<Element, float[]> query) {
        return findSimilarWithDistances(query).stream().map(Pair::first).toList();
    }

    /**
     * Finds elements similar to the given query vector, including their similarity scores.
     * Used by LiSSA for similarity-based matching in the classification phase.
     *
     * @param query The element and vector to find similar elements for
     * @return List of pairs containing similar elements and their similarity scores
     */
    public List<Pair<Element, Float>> findSimilarWithDistances(Pair<Element, float[]> query) {
        return retrievalStrategy.findSimilarElements(query, getAllElementsIntern(true));
    }
}
