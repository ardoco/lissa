package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtType;

import java.util.Objects;

/**
 * Wraps an invocation additionally storing the invoking type.
 */
public final class Invocation {

    /**
     * The actual invocation.
     */
    private final CtInvocation<?> invocation;
    /**
     * The type that invokes the invocation.
     */
    private final CtType<?> invokingType;

    /**
     * Creates a new invocation.
     * @param invocation   the actual invocation to get wrapped.
     */
    public Invocation(CtInvocation<?> invocation) {
        this.invocation = invocation;
        this.invokingType = invocation.getParent(CtType.class);
    }

    /**
     * Returns the actual invocation.
     * @return the actual invocation
     */
    public CtInvocation<?> invocation() {
        return invocation;
    }

    /**
     * Returns the type that invokes the invocation.
     * @return the type that invokes the invocation
     */
    public CtType<?> invokingType() {
        return invokingType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Invocation that = (Invocation) o;
        return invocation.equals(that.invocation) && Objects.equals(invokingType, that.invokingType);
    }

    @Override
    public int hashCode() {
        int result = invocation.hashCode();
        result = 31 * result + Objects.hashCode(invokingType);
        return result;
    }
}
