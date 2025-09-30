package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Knowledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.filter.InvocationFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class Components {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Components.class);
    
    private Components() {
        // utility class
    }
    
    // TODO NamedElementFilter
    // TODO FieldAccessFilter
    // TODO SubInheritanceHierarchyFunction, SubtypeFilter
    // TODO SuperInheritanceHierarchyFunction.DistinctTypeListener

    public static Collection<Component> getComponents(ComponentStrategy[] strategies, CtModel model, ArtifactMapper mapper, List<Path> projectConfigurations, Path codeRoot) {
        Collection<Component> components = new TreeSet<>();
        for (ComponentStrategy strategy : strategies) {
            components.addAll(switch (strategy) {
                case PACKAGES -> getComponents(model, mapper);
                case BUILD_CONFIGURATIONS -> getComponents(projectConfigurations, codeRoot, mapper);
            });
        }
        return components;
    }

    private static Collection<Component> getComponents(List<Path> projectConfigurations, Path codeRoot, ArtifactMapper mapper) {
        Map<Path, SortedSet<Artifact>> componentMapping = new HashMap<>();
        for (Path configPath : projectConfigurations) {
            Path rootPath = configPath.getParent();
            try (Stream<Path> paths = Files.walk(rootPath)) {
                componentMapping.put(rootPath, new TreeSet<>(mapper.getArtifactsByAbsolutePaths(paths)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Collection<Component> components = new TreeSet<>();
        for (Map.Entry<Path, SortedSet<Artifact>> componentEntry : componentMapping.entrySet()) {
            Path rootPath = codeRoot.relativize(componentEntry.getKey());
            if (rootPath.toString().isEmpty() || componentEntry.getValue().isEmpty()) {
                continue;
            }
            SortedSet<String> paths = new TreeSet<>();
            String normalizedPath = rootPath.toString().replace("\\", "/");
            paths.add(normalizedPath);
            components.add(new SimpleComponent(normalizedPath, normalizedPath, componentEntry.getValue(), paths));
        }
        return components;
    }

    public static Collection<ComponentSpoonAdapter> getComponents(CtModel model, ArtifactMapper mapper) {
        // component root packages with contained types
        Map<CtPackage, Collection<CtType<?>>> components = getComponentRootPackages(model);

        Map<CtPackage, SortedSet<String>> packagePaths = new HashMap<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : components.entrySet()) {
            TreeSet<String> collector = new TreeSet<>();
            packagePaths.put(componentEntry.getKey(), collector);
            Collection<Artifact> artifacts = mapper.getArtifacts(componentEntry.getValue());
            if (artifacts.isEmpty()) {
                LOGGER.warn("no mapped artifacts found for component '{}' with types: {}", componentEntry.getKey().getQualifiedName(), 
                        componentEntry.getValue().stream().map(CtType::getQualifiedName).toList());
            }
            for (Artifact artifact : artifacts) {
                String path = artifact.getIdentifier().replaceAll("(?<=%s).*$".formatted(componentEntry.getKey().getQualifiedName().replace(".", "/")), "");
                if (path.isBlank()) {
                    LOGGER.warn("blank relative path for '{}' and '{}'", artifact.getIdentifier(), componentEntry.getKey().getQualifiedName());
                }
                collector.add(path);
            }
        }

        // component providing             these interfaces     to these components, which             invoke (require) them
        //     v                                   v                        v                                   v
        Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>>>
//                providedInterfaces = getProvidedInterfaces(components, model);
                providedInterfaces = new HashMap<>();

        return getComponents(components, packagePaths, providedInterfaces, mapper);
    }

    private static Collection<ComponentSpoonAdapter> getComponents(Map<CtPackage, Collection<CtType<?>>> retriever,
                                                                   Map<CtPackage, SortedSet<String>> packagePaths,
                                                                   Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>>> providedInterfaces,
                                                                   ArtifactMapper mapper) {
        Collection<ComponentSpoonAdapter> components = new HashSet<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : retriever.entrySet()) {
            CtPackage rootPackage = componentEntry.getKey();
            SortedSet<Artifact> containedArtifacts = new TreeSet<>(Comparator.comparing(Knowledge::getIdentifier));
            containedArtifacts.addAll(mapper.getArtifacts(componentEntry.getValue()));
            components.add(new ComponentSpoonAdapter(rootPackage, componentEntry.getValue(), containedArtifacts, packagePaths.get(rootPackage), providedInterfaces.get(rootPackage)));
        }
        return components;
    }
    
    // TODO improve runtime
    // does nothing but setting up maps for fine-grained entities in each nested loop
    private static Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, 
                        Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>
                   >>> getProvidedInterfaces(Map<CtPackage, Collection<CtType<?>>> components, CtModel model) {
        Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>>> providedInterfaces = new HashMap<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : components.entrySet()) {
            CtPackage providingComponent = componentEntry.getKey();
            Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>> providingTypes = new HashMap<>();
            providedInterfaces.put(providingComponent, providingTypes);
            for (CtType<?> providingType : componentEntry.getValue()) {
                Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>> interfaces = new HashMap<>();
                providingTypes.put(providingType, interfaces);
                for (CtExecutableReference<?> executable : providingType.getDeclaredExecutables()) {
                    Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>> requiringComponents = new HashMap<>();
                    interfaces.put(executable, requiringComponents);
                    // iterate over all invocations that invoke this executable
                    for (CtInvocation<?> invocation : model.getElements(new InvocationFilter(executable))) {
                        CtExecutableReference<?> invokingExecutable = invocation.getParent(CtExecutableReference.class);
                        if (invokingExecutable == null) {
                            // TODO deal with overridden methods from inter-project sources
                            continue;
                        }
                        CtType<?> invokingType = invokingExecutable.getParent(CtType.class);
                        for (Map.Entry<CtPackage, Collection<CtType<?>>> otherComponentEntry : components.entrySet()) {
                            if (!providingComponent.equals(otherComponentEntry.getKey()) 
                                    && otherComponentEntry.getValue().contains(invokingType)) {
                                // found component that requires this interface; resolve fine-grained entities
                                requiringComponents.putIfAbsent(otherComponentEntry.getKey(), new HashMap<>());
                                Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>> requiringTypes = requiringComponents.get(otherComponentEntry.getKey());
                                requiringTypes.putIfAbsent(invokingType, new HashMap<>());
                                Map<CtExecutableReference<?>, Collection<CtInvocation<?>>> requiringExecutable = requiringTypes.get(invokingType);
                                requiringExecutable.putIfAbsent(invokingExecutable, new LinkedList<>());
                                requiringExecutable.get(invokingExecutable).add(invocation);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return providedInterfaces;
    }

    private static Map<CtPackage, Collection<CtType<?>>> getComponentRootPackages(CtModel model) {
        MainPackagesRetriever retriever = new MainPackagesRetriever();
        model.getRootPackage().accept(retriever);
        return retriever.packagePaths;
    }

    private static final class MainPackagesRetriever extends CtAbstractVisitor {

        private final Map<CtPackage, Collection<CtType<?>>> packagePaths = new HashMap<>();
        
        @Override
        public void visitCtPackage(CtPackage ctPackage) {
            if (!ctPackage.getTypes().isEmpty()) {
                packagePaths.put(ctPackage, collect(ctPackage));
            } else if (ctPackage.getPackages().size() != 1) {
                Map<CtPackage, Collection<CtType<?>>> subPackageCollector = new HashMap<>();
                for (CtPackage subPackage : ctPackage.getPackages()) {
                    subPackageCollector.put(subPackage, collect(subPackage));
                }
                if (subPackageCollector.values().stream()
                        .filter(types -> !types.isEmpty())
                        .count() > 1) {
                    packagePaths.putAll(subPackageCollector);
                    return;
                }
            }
            
            for (CtPackage subPackage : ctPackage.getPackages()) {
                subPackage.accept(this);
            }
        }

        private Collection<CtType<?>> collect(CtPackage ctPackage) {
            List<CtType<?>> collector = new LinkedList<>(ctPackage.getTypes());
            Queue<CtPackage> toVisit = new LinkedList<>(ctPackage.getPackages());
            while (!toVisit.isEmpty()) {
                CtPackage subPackage = toVisit.poll();
                collector.addAll(subPackage.getTypes());
                toVisit.addAll(subPackage.getPackages());
            }
            return collector;
        }
    }
    
}
