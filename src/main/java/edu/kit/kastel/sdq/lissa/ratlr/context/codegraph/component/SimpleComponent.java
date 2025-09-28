package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.SortedSet;

public class SimpleComponent implements Component {
    
    private final String simpleName;
    private final String qualifiedName;
    private final SortedSet<Artifact> containedArtifacts;
    private final SortedSet<String> paths;

    public SimpleComponent(String simpleName, String qualifiedName, SortedSet<Artifact> containedArtifacts, SortedSet<String> paths) {
        this.simpleName = simpleName;
        this.qualifiedName = qualifiedName;
        this.containedArtifacts = containedArtifacts;
        this.paths = paths;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public SortedSet<Artifact> getContainedArtifacts() {
        return containedArtifacts;
    }

    @Override
    public SortedSet<String> getPaths() {
        return paths;
    }
}
