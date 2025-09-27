package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import spoon.reflect.declaration.CtType;

import java.util.Collection;

public interface TypeContainer {
    
    Collection<CtType<?>> getContainedTypes();
}
