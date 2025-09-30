package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.SortedSet;

public abstract class Component implements Comparable<Component> {
    
    public abstract String getSimpleName();

    public abstract String getQualifiedName();

    public abstract SortedSet<Artifact> getContainedArtifacts();

    public abstract SortedSet<String> getPaths();
    
    @Override
    public int compareTo(Component o) {
        return getQualifiedName().compareTo(o.getQualifiedName());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass() && compareTo((Component) obj) == 0;
    }

    @Override
    public int hashCode() {
        return getQualifiedName().hashCode();
    }
}
