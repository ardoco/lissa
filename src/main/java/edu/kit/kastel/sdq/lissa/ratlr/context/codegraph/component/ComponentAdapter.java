package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.util.Collection;
import java.util.Map;

public class ComponentAdapter implements Component, TypeContainer {
    
    private final CtPackage rootPackage;
    private final Collection<CtType<?>> containedTypes;
    private final Collection<CtType<?>> providedInterfaces;
    private final Collection<CtExecutableReference<?>> providedInterfaceMethods;

    public ComponentAdapter(CtPackage rootPackage, Collection<CtType<?>> containedTypes, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>> ctPackages) {
        this.rootPackage = rootPackage;
        this.containedTypes = containedTypes;
        this.providedInterfaceMethods = null;
        this.providedInterfaces = null;
    }

    public CtPackage getRootPackage() {
        return rootPackage;
    }

    @Override
    public Collection<CtType<?>> getContainedTypes() {
        return containedTypes;
    }

    @Override
    public String getSimpleName() {
        return rootPackage.getSimpleName();
    }
    
    @Override
    public String getQualifiedName() {
        return rootPackage.getQualifiedName();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ComponentAdapter component = (ComponentAdapter) o;
        return rootPackage.equals(component.rootPackage);
    }

    @Override
    public int hashCode() {
        return rootPackage.hashCode();
    }
}
