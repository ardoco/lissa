package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.declaration.CtType;

import java.util.Collection;
import java.util.Map;

public class ArtifactMapper {

    private final Map<Artifact, CtType<?>> typesByArtifact;
    private final Map<TypeDeclaration, Artifact> artifactsByType;

    public ArtifactMapper(Map<Artifact, CtType<?>> typesByArtifact, Map<TypeDeclaration, Artifact> artifactsByType) {
        this.typesByArtifact = typesByArtifact;
        this.artifactsByType = artifactsByType;
    }
    
    public Collection<? extends CtType<?>> getTypes(Collection<Artifact> artifacts) {
        return artifacts.stream()
                .map(typesByArtifact::get)
                .toList();
    }

    public Collection<Artifact> getArtifacts(Collection<CtType<?>> types) {
        return types.stream()
                .map(key -> artifactsByType.get(new TypeDeclaration(key)))
                .toList();
    }
    
}
