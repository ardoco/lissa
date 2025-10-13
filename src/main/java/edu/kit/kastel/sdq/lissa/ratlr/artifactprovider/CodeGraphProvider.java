package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.MultiModelCodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.List;

public class CodeGraphProvider extends PathedProvider {

    private static final String CONTEXT_PREFIX = "codegraph:";
    public static final String CONTEXT_IDENTIFIER = CONTEXT_PREFIX + "codegraph";
    private final ArtifactProvider basicArtifactProvider;
    private final MultiModelCodeGraph codeGraph;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public CodeGraphProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        this.basicArtifactProvider = new RecursiveTextArtifactProvider(Artifact.ArtifactType.SOURCE_CODE, this.path.getPath(), new String[]{".java"}, contextStore);
        // used as a flag for other pipeline components to verify whether this provider is used at configuration validation time
        this.codeGraph = new MultiModelCodeGraph(CONTEXT_IDENTIFIER, this.path.toPath());
        contextStore.createContext(codeGraph);
    }

    @Override
    public List<Artifact> getArtifacts() {
        List<Artifact> artifacts = basicArtifactProvider.getArtifacts();
        codeGraph.setArtifacts(artifacts);
        return artifacts;
    }

    @Override
    public Artifact getArtifact(String identifier) {
        return basicArtifactProvider.getArtifact(identifier);
    }
}
