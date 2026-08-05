/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.RetrievalStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A store for source elements and their embeddings in the LiSSA framework.
 * Providing functionality for element retrieval and filtering
 * <b>Source Store</b> (similarityRetriever = false):
 * <ul>
 *      <li>Used to store source elements that will be used as queries in LiSSA's classification phase</li>
 *      <li>Does not support similarity search as it's unnecessary for source elements</li>
 *      <li>Can retrieve all elements at once for LiSSA's batch processing</li>
 *      <li>Supports filtering elements by comparison flag for LiSSA's selective analysis</li>
 * </ul>
 */
public class SourceElementStore extends ElementStore {

    /**
     * Creates a new source element store for the LiSSA framework.
     *
     * @param moduleConfiguration The configuration of the module
     */
    public SourceElementStore(ModuleConfiguration moduleConfiguration) {
        super();
        if (!"custom".equals(moduleConfiguration.name())) {
            RetrievalStrategy.logger.error(
                    "The element store is created in source store mode, but the retrieval strategy is not set to \"custom\". This is likely a configuration error as source stores do not use retrieval strategies.");
        }
    }

    /**
     * Creates a new source element store with the given content.
     *
     * @param content List of pairs containing elements and their embeddings
     */
    public SourceElementStore(List<Pair<Element, float[]>> content) {
        super(content);
    }

    /**
     * Retrieves all elements in the store for LiSSA's batch processing.
     * Defaults to retrieving all elements, regardless of comparison flag.
     *
     * @return List of all elements
     */
    public List<Element> getAllElements() {
        return getAllElementsIntern(false).stream().map(Pair::first).toList();
    }

    /**
     * Retrieves all elements in the store for LiSSA's batch processing.
     *
     * @param onlyCompare If true, only returns elements marked for comparison
     * @return List of pairs containing elements and their embeddings
     */
    public List<Pair<Element, float[]>> getAllElements(boolean onlyCompare) {
        return getAllElementsIntern(onlyCompare);
    }
}
