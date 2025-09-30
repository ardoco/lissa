package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.SimpleComponent;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public abstract class AbstractCodeGraph implements CodeGraph {
    
    protected final String identifier;
    protected final Path codeRoot;
    protected final List<Path> projectConfigurations;
    protected Collection<Component> components;

    protected AbstractCodeGraph(String identifier, Path codeRoot, List<Path> projectConfigurations) {
        this.identifier = identifier;
        this.codeRoot = codeRoot;
        this.projectConfigurations = projectConfigurations;
    }

    @Override
    public final Collection<Component> getComponents(ComponentStrategy[] strategies) {
        if (components == null) {
            Map<String, List<Component>> collidingComponents = new HashMap<>();
            for (Component component : getComponentsInternal(strategies)) {
                collidingComponents.putIfAbsent(component.getQualifiedName(), new ArrayList<>());
                collidingComponents.get(component.getQualifiedName()).add(component);
            }

            components = new TreeSet<>();
            for (Map.Entry<String, List<Component>> collisionEntry : collidingComponents.entrySet()) {
                if (collisionEntry.getValue().size() == 1) {
                    components.add(collisionEntry.getValue().getFirst());
                    continue;
                }

                SortedSet<Artifact> combinedArtifacts = new TreeSet<>(Comparator.comparing(Artifact::getIdentifier));
                combinedArtifacts.addAll(collisionEntry.getValue().stream()
                        .flatMap(component -> component.getContainedArtifacts().stream())
                        .collect(Collectors.toSet()));
                SortedSet<String> combinedPaths = collisionEntry.getValue().stream()
                        .flatMap(component -> component.getPaths().stream())
                        .collect(Collectors.toCollection(TreeSet::new));
                components.add(new SimpleComponent(collisionEntry.getKey(), collisionEntry.getKey(), combinedArtifacts, combinedPaths));
            }
        }
        return Collections.unmodifiableCollection(components);
    }

    @Override
    public final String getId() {
        return identifier;
    }
    
    protected abstract Collection<Component> getComponentsInternal(ComponentStrategy[] strategies);
    
}
