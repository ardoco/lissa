package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ProjectHierarchyTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Stream;

public class ExtractorPackagesFileExtension extends ComponentExtractor {
    
    private String[] extractionFilters;

    public ExtractorPackagesFileExtension(ModuleConfiguration configuration, ContextStore contextStore, Path codeRoot) {
        super(contextStore, codeRoot);
        String extractionFilters = configuration.argumentAsString("extraction_filters", "");
        if (extractionFilters.isEmpty()) {
            this.extractionFilters = new String[0];
        } else {
            this.extractionFilters = extractionFilters.split(",");
        }
    }

    @Override
    public SortedSet<SimpleComponent> extract() {
        Map<String, SortedSet<Path>> pathsByFileExtension = new HashMap<>();
        try (Stream<Path> walk = Files.walk(codeRoot)) {
            for (Path path : walk.filter(Files::isRegularFile).toList()) {
                String fileExtension = ProjectHierarchyTool.getFileExtension(path);
                pathsByFileExtension.putIfAbsent(fileExtension, new TreeSet<>());
                pathsByFileExtension.get(fileExtension).add(path.getParent());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Map<String, SortedSet<Path>> filtered = filterExtensions(pathsByFileExtension);

        
        
        return null;
    }
    
    private static PathComposite convertToTree(SortedSet<Path> paths) {
        Path current = paths.first();
        for (Path path : paths) {
            
        }
        return null;
    }

    private Map<String, SortedSet<Path>> filterExtensions(Map<String, SortedSet<Path>> pathsByFileExtension) {
        if (extractionFilters.length == 0) {
            return pathsByFileExtension;
        }

        Map<String, SortedSet<Path>> filtered = new HashMap<>();
        for (String filter : extractionFilters) {
            String key = filter;
            if (!pathsByFileExtension.containsKey(key)) {
                if (pathsByFileExtension.containsKey("." + filter)) {
                    key = "." + filter;
                } else {
                    continue;
                }
            }
            filtered.put(key, pathsByFileExtension.get(key));
        }
        return filtered;
    }
    
    private static final class PathComposite {
        private final Collection<PathComposite> children = new LinkedList<>();
        private final String simpleName;
        private PathComposite(String simpleName) {
            this.simpleName = simpleName;
        }
    }
    
    private enum Language {
        JAVA(".java", content -> 
                Arrays.stream(content.split("\r?\n"))
                        .filter(line -> line.matches("^\\s*package\\s.+;\\s*$"))
                        .map(line -> line.replaceAll("(^\\s*package\\s|;\\s*$)", ""))
                        .findFirst()
                        .orElse("-info"));

        private final String extension;
        private final Function<String, String> packageDeclarationExtractor;

        Language(String extension, Function<String, String> packageDeclarationExtractor) {
            this.extension = extension;
            this.packageDeclarationExtractor = packageDeclarationExtractor;
        }
    }
}
