package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.codegraph.ArtifactMappingContext;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.ModelContext;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
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
    public static final String CONTEXT_MODEL = CONTEXT_PREFIX + "model";
    public static final String CONTEXT_CODE_PATH = CONTEXT_PREFIX + "path";
    private final ArtifactProvider basicArtifactProvider;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public CodeGraphProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        Map<String, String> basicConfigArguments = new HashMap<>();
        basicConfigArguments.put("path", this.path.getPath());
        basicConfigArguments.put("artifact_type", Artifact.ArtifactType.SOURCE_CODE.name());
        basicConfigArguments.put("extensions", ".java");
        this.basicArtifactProvider = new RecursiveTextArtifactProvider(new ModuleConfiguration("recursive_text", basicConfigArguments), contextStore);
        contextStore.createContext(new StringContext(CONTEXT_CODE_PATH, this.path.getPath()));
    }

    @Override
    public List<Artifact> getArtifacts() {
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource(this.path.getPath());
        CtModel model = launcher.buildModel();
        contextStore.createContext(new ModelContext(CONTEXT_MODEL, model));
        
        List<Artifact> artifacts = basicArtifactProvider.getArtifacts();
        setArtifactMappingContext(artifacts, model);
        
        return artifacts;
    }

    private void setArtifactMappingContext(List<Artifact> artifacts, CtModel model) {
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
        
        contextStore.createContext(new ArtifactMappingContext(CONTEXT_PREFIX + "artifact_mapping", typeByArtifact, artifactByType));
    }

    @Override
    public Artifact getArtifact(String identifier) {
        return null;
    }
}
