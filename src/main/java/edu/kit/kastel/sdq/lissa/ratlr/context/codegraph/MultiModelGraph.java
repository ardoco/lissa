package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Components;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MultiModelGraph extends AbstractCodeGraph {

    private final Map<CtModel, ArtifactMapper> models;
    private final Collection<Artifact> artifacts;

    public MultiModelGraph(String identifier, Map<CtModel, ArtifactMapper> models, List<Path> projectConfigurations, Path codeRoot) {
        super(identifier, codeRoot, projectConfigurations);
        this.models = models;
        this.artifacts = models.values().stream().flatMap(mapper -> mapper.getArtifacts().stream()).toList();
    }

    @Override
    public Collection<Artifact> getArtifacts() {
        return artifacts;
    }

    @Override
    protected Collection<Component> getComponentsInternal(ComponentStrategy[] strategies) {
        return models.entrySet().stream()
                .flatMap(entry -> Components.getComponents(strategies, entry.getKey(), entry.getValue(), projectConfigurations, codeRoot).stream())
                .toList();
    }
}
