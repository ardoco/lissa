/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.RetrievalStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A store for elements and their embeddings in the LiSSA framework.
 * This class manages a collection of elements and their associated vector embeddings,
 * providing functionality for similarity search and element retrieval as part of
 * LiSSA's trace link analysis approach.
 *
 * The store can operate in two distinct roles within the LiSSA pipeline:
 * <ul>
 *     <li><b>Target Store</b> (similarityRetriever = true):
 *         <ul>
 *             <li>Used to store target elements that will be searched for similarity in LiSSA's classification phase</li>
 *             <li>Cannot retrieve all elements at once</li>
 *         </ul>
 *     </li>
 *     <li><b>Source Store</b> (similarityRetriever = false):
 *         <ul>
 *             <li>Used to store source elements that will be used as queries in LiSSA's classification phase</li>
 *             <li>Does not support similarity search as it's unnecessary for source elements</li>
 *             <li>Can retrieve all elements at once for LiSSA's batch processing</li>
 *             <li>Supports filtering elements by comparison flag for LiSSA's selective analysis</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class ElementStore {

    /**
     * Maps element identifiers to their corresponding elements and embeddings.
     * Used by LiSSA to maintain the relationship between elements and their vector representations.
     */
    private final Map<String, Pair<Element, float[]>> idToElementWithEmbedding;

    /**
     * List of all elements and their embeddings.
     * Used by LiSSA to maintain the order and full set of elements for processing.
     */
    private final List<Pair<Element, float[]>> elementsWithEmbedding;

    /**
     * Strategy to find similar elements.
     * {@code null} indicates source store mode (no similarity search).
     */
    private final RetrievalStrategy retrievalStrategy;

    /**
     * Creates a new element store for the LiSSA framework.
     *
     * @param configuration       The configuration of the module
     * @param similarityRetriever Whether this store should be a target store (true) or source store (false).
     *                            Target stores support similarity search but limit results.
     *                            Source stores allow retrieving all elements but don't support similarity search.
     * @param contextStore
     * @throws IllegalArgumentException If max_results is less than 1 in target store mode
     */
    public ElementStore(ModuleConfiguration configuration, boolean similarityRetriever, ContextStore contextStore) {
        if (similarityRetriever) {
            this.retrievalStrategy = RetrievalStrategy.createStrategy(configuration, contextStore);
        } else {
            if (!"custom".equals(configuration.name())) {
                RetrievalStrategy.logger.error(
                        "The element store is created in source store mode, but the retrieval strategy is not set to \"custom\". This is likely a configuration error as source stores do not use retrieval strategies.");
            }
            this.retrievalStrategy = null;
        }

        elementsWithEmbedding = new ArrayList<>();
        idToElementWithEmbedding = new HashMap<>();
    }

    /**
     * Initializes the element store with elements and their embeddings for LiSSA's processing.
     *
     * @param elements List of elements to store
     * @param embeddings List of embeddings corresponding to the elements
     * @throws IllegalStateException If the store is already initialized
     * @throws IllegalArgumentException If the number of elements and embeddings don't match
     */
    public void setup(List<Element> elements, List<float[]> embeddings) {
        if (!elementsWithEmbedding.isEmpty() || !idToElementWithEmbedding.isEmpty()) {
            throw new IllegalStateException("The element store is already set up.");
        }

        if (elements.size() != embeddings.size()) {
            throw new IllegalArgumentException("The number of elements and embeddings must be equal.");
        }

        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);
            var embedding = embeddings.get(i);
            var pair = new Pair<>(element, embedding);
            elementsWithEmbedding.add(pair);
            idToElementWithEmbedding.put(element.getIdentifier(), pair);
        }
    }

    /**
     * Finds elements similar to the given query vector as part of LiSSA's similarity matching.
     * Only available in target store mode.
     *
     * @param query The element and vector to find similar elements for
     * @return List of similar elements, sorted by similarity
     * @throws IllegalStateException If this is a source store (similarityRetriever = false)
     */
    public final List<Element> findSimilar(Pair<Element, float[]> query) {
        return findSimilarWithDistances(query).stream().map(Pair::first).toList();
    }

    /**
     * Finds elements similar to the given query vector, including their similarity scores.
     * Used by LiSSA for similarity-based matching in the classification phase.
     * Only available in target store mode.
     *
     * @param query The element and vector to find similar elements for
     * @return List of pairs containing similar elements and their similarity scores
     * @throws IllegalStateException If this is a source store (similarityRetriever = false)
     */
    public List<Pair<Element, Float>> findSimilarWithDistances(Pair<Element, float[]> query) {
        if (retrievalStrategy == null) {
            throw new IllegalStateException("You should set retriever to true to activate this feature.");
        }
        return retrievalStrategy.findSimilarElements(query, getAllElementsIntern(true));
    }

    /**
     * Retrieves an element and its embedding by its identifier.
     * Available in both source and target store modes for LiSSA's element lookup.
     *
     * @param id The identifier of the element to retrieve
     * @return A pair containing the element and its embedding, or null if not found
     */
    public Pair<Element, float[]> getById(String id) {
        var element = idToElementWithEmbedding.get(id);
        if (element == null) {
            return null;
        }
        return new Pair<>(element.first(), Arrays.copyOf(element.second(), element.second().length));
    }

    /**
     * Retrieves all elements that have a specific parent element.
     * Available in both source and target store modes for LiSSA's hierarchical analysis.
     *
     * @param parentId The identifier of the parent element
     * @return List of pairs containing elements and their embeddings
     */
    public List<Pair<Element, float[]>> getElementsByParentId(String parentId) {
        List<Pair<Element, float[]>> elements = new ArrayList<>();
        for (Pair<Element, float[]> element : elementsWithEmbedding) {
            if (element.first().getParent() != null
                    && element.first().getParent().getIdentifier().equals(parentId)) {
                elements.add(new Pair<>(element.first(), Arrays.copyOf(element.second(), element.second().length)));
            }
        }
        return elements;
    }

    /**
     * Retrieves all elements in the store for LiSSA's batch processing.
     * Only available in source store mode.
     *
     * @param onlyCompare If true, only returns elements marked for comparison
     * @return List of pairs containing elements and their embeddings
     * @throws IllegalStateException If this is a target store (similarityRetriever = true)
     */
    public List<Pair<Element, float[]>> getAllElements(boolean onlyCompare) {
        if (retrievalStrategy != null) {
            throw new IllegalStateException("You should set retriever to false to activate this feature.");
        }
        return getAllElementsIntern(onlyCompare);
    }

    /**
     * Internal method to retrieve all elements.
     * Available in both source and target store modes for LiSSA's internal processing.
     *
     * @param onlyCompare If true, only returns elements marked for comparison
     * @return List of pairs containing elements and their embeddings
     */
    private List<Pair<Element, float[]>> getAllElementsIntern(boolean onlyCompare) {
        List<Pair<Element, float[]>> elements = new ArrayList<>();
        for (Pair<Element, float[]> element : elementsWithEmbedding) {
            if (!onlyCompare || element.first().isCompare()) {
                elements.add(new Pair<>(element.first(), Arrays.copyOf(element.second(), element.second().length)));
            }
        }
        return elements;
    }
}
