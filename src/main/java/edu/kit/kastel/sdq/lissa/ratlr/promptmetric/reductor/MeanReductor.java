/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.reductor;

import java.util.Collection;

/**
 * A Reductor implementation that calculates the arithmetic mean of a collection of Double values.
 */
public class MeanReductor implements Reductor {

    public MeanReductor() {
        // No specific initialization required
    }

    /**
     * Reduces a collection of Double values by calculating their arithmetic mean.
     * @return The mean of the input values, or 0.0 if the collection is empty.
     */
    @Override
    public double reduce(Collection<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    @Override
    public String getName() {
        return "mean";
    }
}
