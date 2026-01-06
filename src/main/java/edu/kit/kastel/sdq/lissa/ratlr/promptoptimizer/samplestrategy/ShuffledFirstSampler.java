/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

public class ShuffledFirstSampler implements SampleStrategy {

    private final RandomGenerator random;

    public ShuffledFirstSampler(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize) {
        List<T> mutableItems = new ArrayList<>(items);
        Collections.shuffle(mutableItems, random);
        return mutableItems.stream().limit(sampleSize).toList();
    }
}
