package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.declaration.CtType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class ArtifactMapper {

    private final Map<Artifact, CtType<?>> typesByArtifact;
    private final Map<TypeDeclaration, Artifact> artifactsByType;
    private final Map<Path, Artifact> artifactByAbsolutePath;

    public ArtifactMapper(Map<Artifact, CtType<?>> typesByArtifact, Map<TypeDeclaration, Artifact> artifactsByType, Map<Path, Artifact> artifactByAbsolutePath) {
        this.typesByArtifact = typesByArtifact;
        this.artifactsByType = artifactsByType;
        this.artifactByAbsolutePath = artifactByAbsolutePath;
    }
    
    public Collection<? extends CtType<?>> getTypes(Collection<Artifact> artifacts) {
        return artifacts.stream()
                .filter(typesByArtifact::containsKey)
                .map(typesByArtifact::get)
                .toList();
    }

    public Collection<Artifact> getArtifacts(Collection<CtType<?>> types) {
        return types.stream()
                .map(TypeDeclaration::new)
                .filter(artifactsByType::containsKey)
                .map(artifactsByType::get)
                .toList();
    }
    public Collection<Artifact> getArtifactsByAbsolutePaths(Collection<Path> absolutePaths) {
        return getArtifactsByAbsolutePaths(absolutePaths.stream());
    }

    public Collection<Artifact> getArtifactsByAbsolutePaths(Stream<Path> absolutePaths) {
        return absolutePaths.filter(artifactByAbsolutePath::containsKey)
                .map(artifactByAbsolutePath::get)
                .toList();
    }

    public Collection<Artifact> getArtifacts() {
        return typesByArtifact.keySet();
    }
}
