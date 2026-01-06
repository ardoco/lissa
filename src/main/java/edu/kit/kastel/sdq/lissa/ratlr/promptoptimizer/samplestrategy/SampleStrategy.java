/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.Collection;
import java.util.List;

public interface SampleStrategy {
    /**
     * Samples a subset of items from the provided collection.
     * If the sample size exceeds the number of available items, all items are returned.
     *
     * @param items the collection of items to sample from
     * @param sampleSize the number of items to sample
     * @param <T> the type of items in the list
     * @return a list containing the sampled items
     */
    <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize);
}
