/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.random.RandomGenerator;

/**
 * A factory class for creating instances of different sampling strategies based on a provided name.
 */
public final class SamplerFactory {

    public static final String FIRST_SAMPLER = "first";
    public static final String ORDERED_SAMPLER = "ordered";
    public static final String SHUFFLED_SAMPLER = "shuffled";

    private SamplerFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

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
    public static SampleStrategy createSampler(String name, RandomGenerator random) {
        return switch (name) {
            case FIRST_SAMPLER -> new FirstSampler();
            case ORDERED_SAMPLER -> new OrderedFirstSampler();
            case SHUFFLED_SAMPLER -> new ShuffledFirstSampler(random);
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
