package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.CtVisitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CodeGraphProvider extends PathedProvider {

    private final ArtifactProvider basicArtifactProvider;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected CodeGraphProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        Map<String, String> basicConfigArguments = new HashMap<>();
        basicConfigArguments.put("path", this.path.getPath());
        basicConfigArguments.put("artifact_type", Artifact.ArtifactType.SOURCE_CODE.name());
        this.basicArtifactProvider = new RecursiveTextArtifactProvider(new ModuleConfiguration("recursive_text", basicConfigArguments), contextStore);
    }

    @Override
    public List<Artifact> getArtifacts() {
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource(this.path.getPath());
        CtModel model = launcher.buildModel();

        return List.of();
    }

    @Override
    public Artifact getArtifact(String identifier) {
        return null;
    }

    private static final class MethodChainProcessor extends CtAbstractVisitor {
        
        @Override
        public <T> void visitCtExecutableReference(CtExecutableReference<T> reference) {
            System.out.println(reference);
            CtExecutable<T> declaration = reference.getDeclaration();
            if (declaration != null) {
                declaration.accept(this);
            }
        }
    }
}
