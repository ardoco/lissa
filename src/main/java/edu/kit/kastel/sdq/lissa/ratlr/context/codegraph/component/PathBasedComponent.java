package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.BiFunction;

public class PathBasedComponent extends SimpleComponent {

    private final SortedSet<String> paths;
    private final BiFunction<Collection<String>, Collection<Artifact>, Collection<Artifact>> artifactDetermination;

    public PathBasedComponent(String simpleName, String qualifiedName, SortedSet<String> paths, 
                              BiFunction<Collection<String>, Collection<Artifact>, Collection<Artifact>> artifactDetermination) {
        super(simpleName, qualifiedName);
        this.paths = paths;
        this.artifactDetermination = artifactDetermination;
    }

    public PathBasedComponent(String simpleName, String qualifiedName, SortedSet<String> paths) {
        this(simpleName, qualifiedName, paths, PathBasedComponent::filterStartsWith);
    }

    @Override
    public SortedSet<String> getPaths() {
        return paths;
    }

    @Override
    protected Collection<Artifact> determineArtifacts(Collection<Artifact> providedValues) {
        return artifactDetermination.apply(paths, providedValues);
    }
    
    private static Collection<Artifact> filterStartsWith(Collection<String> paths, Collection<Artifact> allArtifacts) {
        return allArtifacts.stream()
                .filter(artifact -> paths.stream().anyMatch(path -> artifact.getIdentifier().startsWith(path)))
                .toList();
    }
}
