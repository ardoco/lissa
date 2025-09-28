package edu.kit.kastel.sdq.lissa.ratlr.context;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentAdapter;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Components;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.CtModel;

import java.util.Collection;
import java.util.Collections;

public class CodeGraph implements Context {

    private final String identifier;
    private final CtModel model;
    private final ArtifactMapper artifactMapper;
    private Collection<ComponentAdapter> components;

    public CodeGraph(String identifier, CtModel model, ArtifactMapper artifactMapper) {
        this.identifier = identifier;
        this.model = model;
        this.artifactMapper = artifactMapper;
    }
    
    public Collection<Component> getComponents() {
        if (components == null) {
            components = Components.getComponents(model, artifactMapper);
        }
        return Collections.unmodifiableCollection(components);
    }
    
    public Collection<Artifact> getContainedArtifacts(Component component) {
        return artifactMapper.getArtifacts(getComponentAdapter(component).getContainedTypes());
    }

    private ComponentAdapter getComponentAdapter(Component component) {
        for (ComponentAdapter adapter : components) {
            if (adapter == component) {
                return adapter;
            }
        }
        throw new IllegalArgumentException("given component is not created by this context");
    }

    @Override
    public String getId() {
        return identifier;
    }
}
