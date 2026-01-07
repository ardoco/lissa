/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.Collection;
import java.util.List;

/**
 * A sampling strategy that selects the first 'n' items from a collection without any sorting or shuffling.
 */
public class FirstSampler implements SampleStrategy {

    /**
     * Samples the first 'sampleSize' items from the provided list as is.
     */
    @Override
    public <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize) {
        return items.stream().limit(sampleSize).toList();
    }
}
