/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric.scorer;

/**
 * Factory class for creating Scorer instances based on provided configurations.
 * This class should not be instantiated; it provides a static method to create
 * Scorer objects.
 */
public final class ScorerFactory {

    private ScorerFactory() {
        throw new IllegalAccessError("Factory class should not be instantiated.");
    }

    /**
     * Creates a Scorer instance based on the provided configuration.
     *
     * @param name The name of the metric type to create.
     * @return A Scorer instance as specified by the configuration.
     * @throws IllegalStateException If the configuration name does not match any known metric types.
     */
    public static Scorer createScorer(String name) {
        return switch (name) {
            case "binary" -> new BinaryScorer();
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
