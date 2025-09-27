package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

public interface Component extends Comparable<Component> {
    
    String getSimpleName();

    String getQualifiedName();
    
    @Override
    default int compareTo(Component o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }
    
}
