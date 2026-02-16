/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * A sampling strategy that shuffles items randomly and then selects the first 'n' items.
 */
public class ShuffledFirstSampler implements SampleStrategy {

    private final RandomGenerator random;

    public ShuffledFirstSampler(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Samples the first 'sampleSize' items from the provided list after shuffling them randomly.
     * If the sample size exceeds the number of available items, all items are returned (shuffled).
     *
     * @param items the collection of items to sample from
     * @param sampleSize the number of items to sample
     * @param <T> the type of items in the collection
     * @return a list containing the sampled items in random order
     */
    @Override
    public <T extends Comparable<T>> List<T> sample(Collection<T> items, int sampleSize) {
        List<T> mutableItems = new ArrayList<>(items);
        Collections.shuffle(mutableItems, random);
        return mutableItems.stream().limit(sampleSize).toList();
    }
}
