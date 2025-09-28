package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.filter.InvocationFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

public final class Components {
    
    private Components() {
        // utility class
    }
    
    // TODO NamedElementFilter
    // TODO FieldAccessFilter
    // TODO SubInheritanceHierarchyFunction, SubtypeFilter
    // TODO SuperInheritanceHierarchyFunction.DistinctTypeListener

    public static Collection<ComponentAdapter> getComponents(CtModel model, ArtifactMapper mapper) {
        // component root packages with contained types
        Map<CtPackage, Collection<CtType<?>>> components = getComponentRootPackages(model);

        Map<CtPackage, SortedSet<String>> packagePaths = new HashMap<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : components.entrySet()) {
            TreeSet<String> collector = new TreeSet<>();
            packagePaths.put(componentEntry.getKey(), collector);
            for (Artifact artifact : mapper.getArtifacts(componentEntry.getValue())) {
                String path = artifact.getIdentifier().replaceAll("(?<=%s).*$".formatted(componentEntry.getKey().getQualifiedName().replace(".", "/")), "");
                collector.add(path);
            }
        }

        // component providing             these interfaces     to these components, which             invoke (require) them
        //     v                                   v                        v                                   v
        Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>>>
//                providedInterfaces = getProvidedInterfaces(components, model);
                providedInterfaces = new HashMap<>();

        return getComponents(components, packagePaths, providedInterfaces);
    }

    private static Collection<ComponentAdapter> getComponents(Map<CtPackage, Collection<CtType<?>>> retriever,
                                                              Map<CtPackage, SortedSet<String>> packagePaths, 
                                                              Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Map<CtPackage, Map<CtType<?>, Map<CtExecutableReference<?>, Collection<CtInvocation<?>>>>>>>> providedInterfaces) {
        Collection<ComponentAdapter> components = new HashSet<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : retriever.entrySet()) {
            CtPackage rootPackage = componentEntry.getKey();
            components.add(new ComponentAdapter(rootPackage, componentEntry.getValue(), packagePaths.get(rootPackage), providedInterfaces.get(rootPackage)));
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
                collect(ctPackage);
            } else if (ctPackage.getPackages().size() != 1) {
                for (CtPackage subPackage : ctPackage.getPackages()) {
                    collect(subPackage);
                }
            } else {
                for (CtPackage subPackage : ctPackage.getPackages()) {
                    subPackage.accept(this);
                }
            }
        }

        private void collect(CtPackage ctPackage) {
            packagePaths.putIfAbsent(ctPackage, new HashSet<>());
            packagePaths.get(ctPackage).addAll(ctPackage.getTypes());
            Queue<CtPackage> toVisit = new LinkedList<>(ctPackage.getPackages());
            while (!toVisit.isEmpty()) {
                CtPackage subPackage = toVisit.poll();
                packagePaths.get(ctPackage).addAll(subPackage.getTypes());
                toVisit.addAll(subPackage.getPackages());
            }
        }
    }
    
}
