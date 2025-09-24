package edu.kit.kastel.sdq.lissa.ratlr.codegraph.component;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.InvocationFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Component {
    
    private final CtPackage rootPackage;
    private final Collection<CtType<?>> containedTypes;
    private final Collection<CtType<?>> providedInterfaces;
    private final Collection<CtExecutableReference<?>> providedInterfaceMethods;

    public Component(CtPackage rootPackage, Collection<CtType<?>> containedTypes) {
        this.rootPackage = rootPackage;
        this.containedTypes = containedTypes;
        this.providedInterfaceMethods = determineUnusedMethods();
        this.providedInterfaces = null;
    }

    private Collection<CtExecutableReference<?>> determineUnusedMethods() {
        Map<CtExecutableReference<?>, Collection<CtInvocation<?>>> invocationsByExecutableTarget = new HashMap<>();
        for (CtType<?> type : containedTypes) {
            for (CtExecutableReference<?> executable : type.getDeclaredExecutables()) {
                invocationsByExecutableTarget.putIfAbsent(executable, new HashSet<>());
                InvocationFilter filter = new InvocationFilter(executable);
//                executable.getDeclaration().getBody().getElements(filter)
            }
        }
        return null;
    }
    
}
