/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.Collection;
import java.util.List;

public class OrderedFirstSampler implements SampleStrategy {

    /**
     * Samples the first 'sampleSize' items from the provided list after sorting them.
     */
    @Override
    public <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize) {
        return items.stream().sorted().limit(sampleSize).toList();
    }
}
