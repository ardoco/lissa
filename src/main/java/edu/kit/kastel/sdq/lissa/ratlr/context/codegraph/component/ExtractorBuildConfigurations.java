package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ExtractorBuildConfigurations extends ComponentExtractor {

    private final List<Path> projectConfigurations;

    public ExtractorBuildConfigurations(ContextStore contextStore, Path codeRoot) {
        super(contextStore, codeRoot);
        this.projectConfigurations = getPOMs();
    }

    @Override
    public SortedSet<SimpleComponent> extract() {
        SortedSet<SimpleComponent> components = new TreeSet<>();
        for (Path configPath : projectConfigurations) {
            Path rootPath = codeRoot.relativize(configPath.getParent());
            if (rootPath.toString().isEmpty()) {
                continue;
            }
            SortedSet<String> paths = new TreeSet<>();
            String normalizedPath = rootPath.toString().replace("\\", "/");
            paths.add(normalizedPath);
            components.add(new PathBasedComponent(normalizedPath, normalizedPath, paths));
        }
        return components;
    }

    private List<Path> getPOMs() {
        try (Stream<Path> walk = Files.walk(this.codeRoot)) {
            return walk.filter(path -> path.getFileName().toString().equals("pom.xml"))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
