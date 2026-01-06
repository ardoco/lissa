/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.reductor;

/**
 * Factory class for creating Reductor instances based on the provided name.
 */
public final class ReductorFactory {

    private ReductorFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

    /**
     * Factory method to create a Reductor based on the provided configuration.
     * The name field indicates the type of Reductor to create.
     *
     * @param name The name of the Reductor type to create.
     * @return An instance of a concrete Reductor implementation.
     * @throws IllegalStateException If the configuration name does not match any known Reductor types.
     */
    public static Reductor createReductor(String name) {
        return switch (name) {
            case "mean" -> new MeanReductor();
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
