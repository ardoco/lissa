/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.Collection;
import java.util.List;

/**
 * A sampling strategy that sorts items and then selects the first 'n' items.
 */
public class OrderedFirstSampler implements SampleStrategy {

    /**
     * Samples the first 'sampleSize' items from the provided list after sorting them.
     * If the sample size exceeds the number of available items, all items are returned sorted.
     */
    @Override
    public <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize) {
        return items.stream().sorted().limit(sampleSize).toList();
    }
}
