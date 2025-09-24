package edu.kit.kastel.sdq.lissa.ratlr.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.CtAbstractVisitor;
import spoon.reflect.visitor.filter.InvocationFilter;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ComponentDetector {

    public static void detectComponents(ContextStore contextStore, CtModel model) {
        MainPackagesRetriever retriever = new MainPackagesRetriever();
        model.getRootPackage().accept(retriever);

        Map<TypeDeclaration, Map<CtExecutableReference<?>, List<CtInvocation<?>>>> invocationsByExecutableTargetByDeclaringType = new HashMap<>();
        for (CtType<?> type : model.getAllTypes()) {
            TypeDeclaration typeDeclaration = new TypeDeclaration(type);
            HashMap<CtExecutableReference<?>, List<CtInvocation<?>>> executableMap = new HashMap<>();
            invocationsByExecutableTargetByDeclaringType.put(typeDeclaration, executableMap);
            
            for (CtExecutableReference<?> executable : type.getDeclaredExecutables()) {
                executableMap.put(executable, model.getElements(new InvocationFilter(executable)));
            }
        }

        Map<CtPackage, Collection<Pair<CtInvocation<?>, CtType<?>>>> invocationsRequiringInterfaceByProvidingComponent = new HashMap<>();
        for (Map.Entry<CtPackage, Collection<CtType<?>>> componentEntry : retriever.packagePaths.entrySet()) {
            
            Collection<Pair<CtInvocation<?>, CtType<?>>> invocationsRequiringInterface = new LinkedList<>();
            for (CtType<?> containedType : componentEntry.getValue()) {
                
                for (Map.Entry<CtExecutableReference<?>, List<CtInvocation<?>>> entry : invocationsByExecutableTargetByDeclaringType.get(new TypeDeclaration(containedType))
                        .entrySet()) {
                    
                    // retrieve inter-component invocations that invoke executables of the current component
                    invocationsRequiringInterface.addAll(entry.getValue().stream()
                            .map(invocation -> new Pair<CtInvocation<?>, CtType<?>>(invocation, invocation.getParent(CtType.class)))
                            .filter(pair -> !componentEntry.getValue().contains(pair.second()))
                            .toList());
                }
            }
            invocationsRequiringInterfaceByProvidingComponent.put(componentEntry.getKey(), invocationsRequiringInterface);
        }

        Map<CtPackage, Collection<CtPackage>> providesInterfaceConnections = new HashMap<>();
        for (Map.Entry<CtPackage, Collection<Pair<CtInvocation<?>, CtType<?>>>> invocationRequiringInterfaceEntry 
                : invocationsRequiringInterfaceByProvidingComponent.entrySet()) {
            providesInterfaceConnections.put(invocationRequiringInterfaceEntry.getKey(), new HashSet<>());
            for (Pair<CtInvocation<?>, CtType<?>> invocationWithDeclaringType : invocationRequiringInterfaceEntry.getValue()) {
                for (Map.Entry<CtPackage, Collection<CtType<?>>> componentTypesEntry : retriever.packagePaths.entrySet()) {
                    if (componentTypesEntry.getValue().contains(invocationWithDeclaringType.second())) {
                        providesInterfaceConnections.get(invocationRequiringInterfaceEntry.getKey()).add(componentTypesEntry.getKey());
                        break;
                    }
                }
            }
        }
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

        private Collection<Path> collectPackagePaths(CtPackage ctPackage) {
            Collection<Path> paths = new HashSet<>();
            for (CtType<?> type : ctPackage.getTypes()) {
                paths.add(type.getPosition().getFile().toPath());
            }
            return paths;
        }
    }
    
}
