/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy;

import java.util.random.RandomGenerator;

public final class SamplerFactory {

    public static final String FIRST_SAMPLER = "first";
    public static final String ORDERED_SAMPLER = "ordered";
    public static final String SHUFFLED_SAMPLER = "shuffled";

    private SamplerFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

    public static SampleStrategy createSampler(String name, RandomGenerator random) {
        return switch (name) {
            case FIRST_SAMPLER -> new FirstSampler();
            case ORDERED_SAMPLER -> new OrderedFirstSampler();
            case SHUFFLED_SAMPLER -> new ShuffledFirstSampler(random);
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
