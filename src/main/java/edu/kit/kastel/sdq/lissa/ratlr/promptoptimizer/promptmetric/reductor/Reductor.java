/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.reductor;

import java.util.Collection;

/**
 * Interface for reducing a collection of Double values to a single Double value.
 */
public interface Reductor {

    /**
     * Reduces a collection of Double values to a single Double value.
     *
     * @param values The collection of Double values to be reduced.
     * @return A single Double value representing the reduction of the input values.
     */
    double reduce(Collection<Double> values);

    /**
     * Returns the name of the Reductor implementation.
     *
     * @return The name of the Reductor.
     */
    String getName();

    /**
     * Factory method to create a Reductor based on the provided configuration.
     * The name field indicates the type of Reductor to create.
     *
     * @param name The name of the Reductor type to create.
     * @return An instance of a concrete Reductor implementation.
     * @throws IllegalStateException If the configuration name does not match any known Reductor types.
     */
    static Reductor createReductor(String name) {
        return switch (name) {
            case "mean" -> new MeanReductor();
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
