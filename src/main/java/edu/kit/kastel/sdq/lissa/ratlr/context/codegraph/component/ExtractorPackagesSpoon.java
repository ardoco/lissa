package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ArtifactMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.types.TypeDeclaration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.CtAbstractVisitor;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ExtractorPackagesSpoon extends ComponentExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtractorPackagesSpoon.class);
    private final Collection<CtModel> models;

    public ExtractorPackagesSpoon(Collection<CtModel> models, ContextStore contextStore, Path codeRoot) {
        super(contextStore, codeRoot);
        this.models = models;
    }

    @Override
    public SortedSet<SimpleComponent> extract() {
        Map<CtPackage, Collection<CtModel>> modelsByRootPackage = new HashMap<>();
        Map<CtPackage, SortedSet<String>> pathsByRootPackage = new HashMap<>();
        for (CtModel model : models) {
            Map<CtPackage, Collection<CtType<?>>> componentRootPackages = getComponentRootPackages(model);
            for (Map.Entry<CtPackage, Collection<CtType<?>>> entry : componentRootPackages.entrySet()) {
                modelsByRootPackage.putIfAbsent(entry.getKey(), new ArrayList<>());
                modelsByRootPackage.get(entry.getKey()).add(model);

                pathsByRootPackage.putIfAbsent(entry.getKey(), new TreeSet<>());
                pathsByRootPackage.get(entry.getKey()).addAll(extractPaths(entry.getKey(), entry.getValue()));
            }
        }
        
        return getComponents(pathsByRootPackage, modelsByRootPackage);
    }

    private static SortedSet<SimpleComponent> getComponents(Map<CtPackage, SortedSet<String>> pathsByRootPackage, 
                                               Map<CtPackage, Collection<CtModel>> modelsByRootPackage) {
        return pathsByRootPackage.entrySet().stream()
                .map(entry -> new ModelBasedComponent(entry.getKey().getSimpleName(), 
                        entry.getKey().getQualifiedName(), entry.getValue(), modelsByRootPackage.get(entry.getKey())))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private SortedSet<String> extractPaths(CtPackage ctPackage, Collection<CtType<?>> types) {
        String pathDelimiter = codeRoot.toString().replace("\\", "/").replaceAll("^\\.", ".*");
        SortedSet<String> paths = new TreeSet<>();
        for (CtType<?> type : types) {
            File file = type.getPosition().getFile();
            if (file == null) {
                continue;
            }
            String typePath = file.getPath();
            String[] split = typePath.replace("\\", "/").split(pathDelimiter);
            String relativeTypePath;
            if (split.length == 2) {
                relativeTypePath = split[1];
            } else {
                throw new IllegalStateException("unable to relativize type path: " + typePath);
            }
            String artifactId = relativeTypePath.replaceAll("^/", "");

            String path = artifactId.replaceAll("(?<=%s).*$".formatted(ctPackage.getQualifiedName().replace(".", "/")), "");
            if (path.isBlank()) {
                LOGGER.warn("blank relative path for '{}' and '{}'", artifactId, ctPackage.getQualifiedName());
            }
            paths.add(path);
        }
        return paths;
    }

    private ArtifactMapper getArtifactMapping(List<Artifact> artifacts, CtModel model) {
        Map<String, Artifact> artifactByPath = artifacts.stream().collect(Collectors.toMap(Artifact::getIdentifier, artifact -> artifact));
        String pathDelimiter = codeRoot.toString().replace("\\", "/").replaceAll("^\\.", ".*");

        Map<Artifact, CtType<?>> typeByArtifact = new HashMap<>();
        Map<TypeDeclaration, Artifact> artifactByType = new HashMap<>();
        Map<Path, Artifact> artifactByAbsolutePath = new HashMap<>();
        Collection<CtType<?>> allTypes = model.getAllTypes();
//        logger.info("found {} types", allTypes.size());
        for (CtType<?> type : allTypes) {
            File file = type.getPosition().getFile();
            if (file == null) {
//                logger.warn("no source position for type '{}'", type.getQualifiedName());
                continue;
            }
            String typePath = file.getPath();
            String[] split = typePath.replace("\\", "/").split(pathDelimiter);
            String relativeTypePath;
            if (split.length == 2) {
                relativeTypePath = split[1];
            } else {
                throw new IllegalStateException("unable to relativize type path: " + typePath);
            }

            Artifact artifact = artifactByPath.get(relativeTypePath.replaceAll("^/", ""));
            if (typeByArtifact.containsKey(artifact)) {
                throw new IllegalStateException("type '%s' collides with type '%s' for artifact '%s"
                        .formatted(type.getQualifiedName(), typeByArtifact.get(artifact).getQualifiedName(), artifact));
            }

            typeByArtifact.put(artifact, type);
            artifactByType.put(new TypeDeclaration(type), artifact);
            artifactByAbsolutePath.put(file.toPath(), artifact);
        }

//        if (artifacts.size() != typeByArtifact.size() 
//                && artifacts.stream().anyMatch(artifact -> !typeByArtifact.containsKey(artifact) 
//                                                                    && !artifact.getIdentifier().endsWith("module-info.java") 
//                                                                    && !artifact.getIdentifier().endsWith("package-info.java"))) {
//            throw new IllegalStateException("expected to map %d artifacts, but mapped %d".formatted(artifacts.size(), typeByArtifact.size()));
//        }

        return new ArtifactMapper(typeByArtifact, artifactByType, artifactByAbsolutePath);
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
