package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.util.Collection;
import java.util.Map;

public class ProvidedInterface {
    
    private final CtExecutableReference<?> executable;
    private final Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>> requiringComponents;

    public ProvidedInterface(CtExecutableReference<?> executable, 
                             Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>> requiringComponents) {
        this.executable = executable;
        this.requiringComponents = requiringComponents;
    }
    
    void addRequiring() {
        
    }
}
