/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.reductor;

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
}
