/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.context;

/**
 * Represents a context object that can be shared across pipeline components in the LiSSA framework.
 * Contexts are used to store and provide additional information or state that may be required by
 * various components (e.g., artifact providers, preprocessors, classifiers) during pipeline execution.
 * Each context must have a unique identifier.
 */
public interface Context {
    /**
     * Returns the unique identifier for this context.
     *
     * @return the context's unique ID
     */
    String getId();
}
