package edu.kit.kastel.sdq.lissa.ratlr.context;

/**
 * Defines the context to be serializable.
 */
public interface SerializedContext extends Context {

    /**
     * Returns the serialized context.
     * 
     * @return the serialized context
     */
    String asString();
}
