package edu.kit.kastel.sdq.lissa.ratlr.codegraph.types;

import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;

import java.util.Objects;

public class TypeDeclaration {
    
    // TODO consider other parent because test modules may contain the same package and inside a single package two identical inner types may be declared 
    private final CtPackage parentPackage;
    private final CtType<?> declaringType;

    public TypeDeclaration(CtType<?> type) {
        this.declaringType = type;
        this.parentPackage = type.getPackage();
    }

    public CtPackage getParentPackage() {
        return parentPackage;
    }

    public CtType<?> getDeclaringType() {
        return declaringType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        TypeDeclaration that = (TypeDeclaration) o;
        return Objects.equals(parentPackage, that.parentPackage) && Objects.equals(declaringType, that.declaringType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentPackage, declaringType);
    }

    @Override
    public String toString() {
        return declaringType.getQualifiedName();
    }
}
