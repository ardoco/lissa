package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Components;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SingleModelGraph extends AbstractCodeGraph {

    private final CtModel model;
    private final ArtifactMapper artifactMapper;

    public SingleModelGraph(String identifier, CtModel model, ArtifactMapper artifactMapper, List<Path> projectConfigurations, Path codeRoot) {
        super(identifier, codeRoot, projectConfigurations);
        this.model = model;
        this.artifactMapper = artifactMapper;
    }

    public List<Path> getProjectConfigurations() {
        return Collections.unmodifiableList(projectConfigurations);
    }

    @Override
    protected Collection<Component> getComponentsInternal(ComponentStrategy[] strategies) {
        return Components.getComponents(strategies, model, artifactMapper, projectConfigurations, codeRoot);
    }

    @Override
    public Collection<Artifact> getArtifacts() {
        return artifactMapper.getArtifacts();
    }
    
}
