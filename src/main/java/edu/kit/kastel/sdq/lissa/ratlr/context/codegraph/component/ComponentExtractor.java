package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;

import java.nio.file.Path;
import java.util.SortedSet;

public abstract class ComponentExtractor {

    protected final ContextStore contextStore;
    protected final Path codeRoot;

    protected ComponentExtractor(ContextStore contextStore, Path codeRoot) {
        this.codeRoot = codeRoot;
        if (!contextStore.hasContext(CodeGraphProvider.CONTEXT_IDENTIFIER)) {
            throw new IllegalArgumentException("artifact provider must be '%s'".formatted("code_graph"));
        }
        this.contextStore = contextStore;
    }
    
    public abstract SortedSet<SimpleComponent> extract();
}
