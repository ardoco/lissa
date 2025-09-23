package edu.kit.kastel.sdq.lissa.ratlr.context;

/**
 * This class represents an implementation of a {@link Context} in which the context is a simple String.
 * It is assumed that the representation of this String is the String itself; {@link #asString()} thereby returns the context.
 */
public class StringContext implements SerializedContext {
    
    private final String id;
    private final String context;

    /**
     * Creates a new instance.
     * 
     * @param id the id of this context
     * @param context the context to be stored
     */
    public StringContext(String id, String context) {
        this.id = id;
        this.context = context;
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Returns the provided context itself.
     * 
     * @return the provided context itself
     */
    @Override
    public String asString() {
        return this.context;
    }
}
