package edu.kit.kastel.sdq.lissa.ratlr.codegraph.component;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.util.Collection;
import java.util.Map;

public class Component {
    
    private final CtPackage rootPackage;
    private final Collection<CtType<?>> containedTypes;
    private final Collection<CtType<?>> providedInterfaces;
    private final Collection<CtExecutableReference<?>> providedInterfaceMethods;

    public Component(CtPackage rootPackage, Collection<CtType<?>> containedTypes, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>> ctPackages) {
        this.rootPackage = rootPackage;
        this.containedTypes = containedTypes;
        this.providedInterfaceMethods = null;
        this.providedInterfaces = null;
    }

    public CtPackage getRootPackage() {
        return rootPackage;
    }

    public Collection<CtType<?>> getContainedTypes() {
        return containedTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Component component = (Component) o;
        return rootPackage.equals(component.rootPackage);
    }

    @Override
    public int hashCode() {
        return rootPackage.hashCode();
    }
}
