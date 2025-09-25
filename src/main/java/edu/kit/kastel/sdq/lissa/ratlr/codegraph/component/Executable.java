package edu.kit.kastel.sdq.lissa.ratlr.codegraph.component;

import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.util.Objects;

/**
 * Wraps an executable reference (method or constructor) additionally storing the declaring type.
 */
public class Executable {

    /**
     * The actual executable reference.
     */
    private final CtExecutableReference<?> executable;
    /**
     * The type declaring the executable reference.
     */
    private final CtType<?> declaringType;

    /**
     * Creates a new executable.
     * @param executable the actual executable to get wrapped.
     */
    public Executable(CtExecutableReference<?> executable) {
        this.executable = executable;
        this.declaringType = executable.getParent(CtType.class);
    }

    /**
     * Returns the actual executable reference.
     * @return the actual executable reference
     */
    public CtExecutableReference<?> getExecutable() {
        return executable;
    }

    /**
     * Returns the type declaring the executable reference.
     * @return the type declaring the executable reference
     */
    public CtType<?> getDeclaringType() {
        return declaringType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Executable that = (Executable) o;
        return executable.equals(that.executable) && Objects.equals(declaringType, that.declaringType);
    }

    @Override
    public int hashCode() {
        int result = executable.hashCode();
        result = 31 * result + Objects.hashCode(declaringType);
        return result;
    }
}
