package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CodeGraphProvider extends PathedProvider {

    public static final String CONTEXT_PREFIX = "codegraph:";
    public static final String CONTEXT_IDENTIFIER = CONTEXT_PREFIX + "codegraph";
    public static final String CONTEXT_CODE_PATH_IDENTIFIER = CONTEXT_PREFIX + "code_root_path";
    private final ArtifactProvider basicArtifactProvider;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public CodeGraphProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        this.basicArtifactProvider = new RecursiveTextArtifactProvider(Artifact.ArtifactType.SOURCE_CODE, this.path.getPath(), new String[]{".java"}, contextStore);
        // used as a flag for other pipeline components to verify whether this provider is used at configuration validation time
        contextStore.createContext(new StringContext(CONTEXT_CODE_PATH_IDENTIFIER, this.path.getPath()));
    }

    @Override
    public List<Artifact> getArtifacts() {
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource(this.path.getPath());
        CtModel model = launcher.buildModel();
        
        List<Artifact> artifacts = basicArtifactProvider.getArtifacts();
        var artifactMapping = getArtifactMapping(artifacts, model);
        contextStore.createContext(new CodeGraph(CONTEXT_IDENTIFIER, model, new ArtifactMapper(artifactMapping.first(), artifactMapping.second())));

        return artifacts;
    }

    private Pair<Map<Artifact, CtType<?>>, Map<TypeDeclaration, Artifact>> getArtifactMapping(List<Artifact> artifacts, CtModel model) {
        Map<String, Artifact> artifactByPath = artifacts.stream().collect(Collectors.toMap(Artifact::getIdentifier, artifact -> artifact));
        String pathDelimiter = this.path.getPath().replace("\\", "/").replaceAll("^\\.", ".*");

        Map<Artifact, CtType<?>> typeByArtifact = new HashMap<>();
        Map<TypeDeclaration, Artifact> artifactByType = new HashMap<>();
        for (CtType<?> type : model.getAllTypes()) {
            String typePath = type.getPosition().getFile().getPath();
            String[] split = typePath.replace("\\", "/").split(pathDelimiter);
            String relativeTypePath;
            if (split.length == 2) {
                relativeTypePath = split[1];
            } else {
                throw new IllegalStateException("unable to relativize type path: " + typePath);
            }

            Artifact artifact = artifactByPath.get(relativeTypePath.replaceAll("^/", ""));
            if (typeByArtifact.containsKey(artifact)) {
                throw new IllegalStateException("type '%s' collides with type '%s' for artifact '%s"
                        .formatted(type.getQualifiedName(), typeByArtifact.get(artifact).getQualifiedName(), artifact));
            }

            typeByArtifact.put(artifact, type);
            artifactByType.put(new TypeDeclaration(type), artifact);
        }
        
        if (artifacts.size() != typeByArtifact.size()) {
            throw new IllegalStateException("expected to map %d artifacts, but mapped %d".formatted(artifacts.size(), typeByArtifact.size()));
        }

        return new Pair<>(typeByArtifact, artifactByType);
    }

    @Override
    public Artifact getArtifact(String identifier) {
        return basicArtifactProvider.getArtifact(identifier);
    }
}
