/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;

public interface SampleStrategy {

    String FIRST_SAMPLER = "first";
    String ORDERED_SAMPLER = "ordered";
    String SHUFFLED_SAMPLER = "shuffled";

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

    /**
     * Creates a new instance of a sampling strategy based on the provided name.
     * The following strategies are supported:
     * <ul>
     *     <li><b>{@value FIRST_SAMPLER}</b>: Selects the first 'n' items from the collection as is.</li>
     *     <li><b>{@value ORDERED_SAMPLER}</b>: Sorts the items and then selects the first 'n' items.</li>
     *     <li><b>{@value SHUFFLED_SAMPLER}</b>: Randomly shuffles the items and then selects the first 'n' items.</li>
     * </ul>
     *
     * @param name the name of the sampling strategy to create
     * @param random a RandomGenerator instance for strategies that require randomness
     * @return an instance of the specified sampling strategy
     * @throws IllegalStateException if the provided name does not match any known sampling strategy
     */
    static SampleStrategy createSampler(String name, RandomGenerator random) {
        return switch (name) {
            case FIRST_SAMPLER -> new FirstSampler();
            case ORDERED_SAMPLER -> new OrderedFirstSampler();
            case SHUFFLED_SAMPLER -> new ShuffledFirstSampler(random);
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
