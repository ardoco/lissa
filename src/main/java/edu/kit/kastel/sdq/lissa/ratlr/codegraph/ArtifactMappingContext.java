package edu.kit.kastel.sdq.lissa.ratlr.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.context.Context;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.declaration.CtType;

import java.util.Collections;
import java.util.Map;

public class ArtifactMappingContext implements Context {
    private final String identifier;
    private final Map<Artifact, CtType<?>> typesByArtifact;
    private final Map<TypeDeclaration, Artifact> artifactsByType;

    public ArtifactMappingContext(String identifier, Map<Artifact, CtType<?>> typesByArtifact, Map<TypeDeclaration, Artifact> artifactsByType) {
        this.identifier = identifier;
        this.typesByArtifact = typesByArtifact;
        this.artifactsByType = artifactsByType;
    }

    public Map<Artifact, CtType<?>> getTypesByArtifact() {
        return Collections.unmodifiableMap(typesByArtifact);
    }

    public Map<TypeDeclaration, Artifact> getArtifactsByType() {
        return Collections.unmodifiableMap(artifactsByType);
    }

    @Override
    public String getId() {
        return identifier;
    }
}
