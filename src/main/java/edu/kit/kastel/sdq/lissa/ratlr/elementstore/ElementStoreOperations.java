/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.elementstore;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

/**
 * A utility class for operations related to ElementStores.
 */
public final class ElementStoreOperations {
    private ElementStoreOperations() {
        throw new IllegalAccessError();
    }

    /**
     * Retrieves a subset of this source store to be used as training data for optimization.
     * The training data consists of the first size elements from the source store.
     *
     * @param size The number of elements to include in the training source store
     * @return A new ElementStore containing only the training data elements
     */
    public static SourceElementStore reduceSourceElementStore(SourceElementStore sourceElementStore, int size) {
        return new SourceElementStore(
                sourceElementStore.getAllElements(false).subList(0, Math.min(size, sourceElementStore.size())));
    }

    /**
     * Retrieves a subset of this target store that corresponds to the source store.
     * This method finds all elements in this target store that are similar to the elements in the source store.
     *
     * @param sourceStore The training source element store
     * @return A new ElementStore containing only the target elements that correspond to the source elements
     * TODO: This adds duplicates if multiple source elements map to the same target elements. However changeing this
     *       would change the behavior of LiSSA's optimization.
     */
    public static TargetElementStore reduceTargetElementStore(
            TargetElementStore targetElementStore, SourceElementStore sourceStore) {
        List<Pair<Element, float[]>> reducedTargetElements = new ArrayList<>();
        for (var element : sourceStore.getAllElements(true)) {
            for (Element candidate : targetElementStore.findSimilar(element)) {
                reducedTargetElements.add(targetElementStore.getById(candidate.getIdentifier()));
            }
        }
        return new TargetElementStore(reducedTargetElements, targetElementStore.getRetrievalStrategy());
    }
}
