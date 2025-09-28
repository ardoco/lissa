package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import java.util.SortedSet;

public interface Component extends Comparable<Component> {
    
    String getSimpleName();

    String getQualifiedName();

    SortedSet<String> getPaths();
    
    @Override
    default int compareTo(Component o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }
    
}
