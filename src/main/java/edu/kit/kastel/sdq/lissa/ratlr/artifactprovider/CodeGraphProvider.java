package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.MultiModelCodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.List;

public class CodeGraphProvider extends RecursiveTextArtifactProvider {

    private final MultiModelCodeGraph codeGraph;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public CodeGraphProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(ensureCorrectArtifactType(configuration), contextStore);
        // used as a flag for other pipeline components to verify whether this provider is used at configuration validation time
        this.codeGraph = new MultiModelCodeGraph(this.path.toPath());
        contextStore.createContext(codeGraph);
    }

    @Override
    public List<Artifact> getArtifacts() {
        List<Artifact> artifacts = super.getArtifacts();
        codeGraph.setArtifacts(artifacts);
        return artifacts;
    }
    
    private static ModuleConfiguration ensureCorrectArtifactType(ModuleConfiguration configuration) {
        if (configuration.hasArgument(ARTIFACT_TYPE_KEY)) {
            if (!Artifact.ArtifactType.SOURCE_CODE.equals(Artifact.ArtifactType.from(configuration.argumentAsString(ARTIFACT_TYPE_KEY)))) {
                throw new IllegalArgumentException("The argument key '%s' is expected to be '%s' as this provider works with source code"
                        .formatted(ARTIFACT_TYPE_KEY, Artifact.ArtifactType.SOURCE_CODE));
            }
        }
        // set the default value to be serialized, must happen before TextArtifactProvider accesses it, hence this method
        configuration.injectArgument(ARTIFACT_TYPE_KEY, Artifact.ArtifactType.SOURCE_CODE.name());
        return configuration;
    }
}
