package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentExtractor;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ComponentStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ExtractorBuildConfigurations;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ExtractorDirectories;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.ExtractorPackagesSpoon;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.PathBasedComponent;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.SimpleComponent;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.SpoonException;
import spoon.reflect.CtModel;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MultiModelCodeGraph implements CodeGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiModelCodeGraph.class);
    private final Path codeRoot;
    private final Set<ComponentExtractor> componentExtractors = new LinkedHashSet<>();
    private Collection<CtModel> models;
    private List<Artifact> artifacts;
    private Collection<Component> components;

    public MultiModelCodeGraph(Path codeRoot) {
        this.codeRoot = codeRoot;
    }
    
    @Override
    public final void initializeComponentExtraction(ModuleConfiguration configuration, ContextStore contextStore) {
        Set<ComponentStrategy> extractionStrategies = new LinkedHashSet<>();
        String[] strategySplit = configuration.argumentAsString("strategies").split(",");
        for (String strategy : strategySplit) {
            if (!extractionStrategies.add(ComponentStrategy.valueOf(strategy))) {
                throw new IllegalArgumentException("component extraction strategy '%s' already defined".formatted(strategy));
            }
        }
        for (ComponentStrategy strategy : extractionStrategies) {
            componentExtractors.add(switch (strategy) {
                case BUILD_CONFIGURATIONS -> new ExtractorBuildConfigurations(contextStore, codeRoot);
                case AI_DIRECTORY -> new ExtractorDirectories(configuration, contextStore, codeRoot);
                case PACKAGES -> new ExtractorPackagesSpoon(getModels(), contextStore, codeRoot);
            });
        }
    }

    @Override
    public final Collection<Component> getComponents() {
        if (artifacts == null) {
            throw new IllegalStateException("artifacts must be set first");
        } else if (componentExtractors.isEmpty()) {
            throw new IllegalStateException("component extraction strategies must be initialized first");
        }

        if (components == null) {
            Map<String, List<Component>> collidingComponents = new HashMap<>();
            for (SimpleComponent component : componentExtractors.stream()
                    .flatMap(extractor -> extractor.extract().stream())
                    .toList()) {
                collidingComponents.putIfAbsent(component.getQualifiedName(), new ArrayList<>());
                collidingComponents.get(component.getQualifiedName()).add(component);
                component.determineContainedArtifacts(artifacts);
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
                PathBasedComponent component = new PathBasedComponent(collisionEntry.getKey(), collisionEntry.getKey(), combinedPaths, 
                        (ignored, artifacts) -> artifacts);
                component.determineContainedArtifacts(combinedArtifacts);
                components.add(component);
            }
        }
        return Collections.unmodifiableCollection(components);
    }

    @Override
    public Collection<Artifact> getArtifacts() {
        return Collections.unmodifiableCollection(artifacts);
    }

    @Override
    public Path getCodeRoot() {
        return codeRoot;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    @Override
    public final String getId() {
        return CodeGraph.CONTEXT_IDENTIFIER;
    }

    private Collection<CtModel> getModels() {
        if (models == null) {
            models = getModels(codeRoot);
            if (models.isEmpty()) {
                LOGGER.info("unable to build any Spoon model");
            }
        }
        return Collections.unmodifiableCollection(models);
    }

    private Collection<CtModel> getModels(Path resourcePath) {
        Collection<CtModel> collector = new ArrayList<>();
        SpoonAPI launcher = new Launcher();
        launcher.addInputResource(resourcePath.toString());
        launcher.getEnvironment().setIgnoreSyntaxErrors(true); // needed for wrongly handled package-info
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        try {
            CtModel model = launcher.buildModel();
            if (!model.getAllTypes().isEmpty()) {
                collector.add(model);
            }
        } catch (SpoonException e) {
            if (e.getMessage().startsWith("Ambiguous package name detected.")) {
                LOGGER.info("resource '{}' contains ambiguous packages, try building multiple models from subfolders", resourcePath);
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resourcePath)) {
                    for (Path directoryEntry : directoryStream) {
                        if (Files.isDirectory(directoryEntry)) {
                            collector.addAll(getModels(directoryEntry));
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw e;
            }
        }
        return collector;
    }
    
}
