package edu.kit.kastel.sdq.lissa.ratlr.context;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentSpoonAdapter;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Components;
import spoon.reflect.CtModel;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CodeGraph implements Context {

    private final String identifier;
    private final CtModel model;
    private final ArtifactMapper artifactMapper;
    private final List<Path> projectConfigurations;
    private Collection<Component> components;
    private final Path codeRoot;

    public CodeGraph(String identifier, CtModel model, ArtifactMapper artifactMapper, List<Path> projectConfigurations, Path codeRoot) {
        this.identifier = identifier;
        this.model = model;
        this.artifactMapper = artifactMapper;
        this.projectConfigurations = projectConfigurations;
        this.codeRoot = codeRoot;
    }

    public List<Path> getProjectConfigurations() {
        return Collections.unmodifiableList(projectConfigurations);
    }

    public Collection<Component> getComponents(ComponentStrategy[] strategies) {
        if (components == null) {
            components = Components.getComponents(strategies, model, artifactMapper, projectConfigurations, codeRoot);
        }
        return Collections.unmodifiableCollection(components);
    }

    @Override
    public String getId() {
        return identifier;
    }
}
