package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;
import edu.kit.kastel.sdq.lissa.ratlr.utils.tools.ProgrammaticToolProvider;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectHierarchyTool implements ProgrammaticToolProvider {

    private final Path root;

    public ProjectHierarchyTool(Path root) {
        this.root = root;
    }

    @Override
    public List<Method> getToolMethods() throws NoSuchMethodException {
        return List.of(getClass().getMethod("showSubDirectoriesOfRoot")
                , getClass().getMethod("showSubDirectoryOfDirectory", String.class)
                , getClass().getMethod("fileExtensionsSummary", String.class)
                , getClass().getMethod("fileExtensionDirectories", String.class, String.class)
                , getClass().getMethod("validateDirectory", String.class));
    }

    @Tool("Returns sub-directories of the root folder of the project up to two levels deep.")
    public String showSubDirectoriesOfRoot() throws IOException {
        return showSubDirectoryOfDirectory(".");
    }

    @Tool("Returns sub-directories of the given directory up to two levels deep.")
    public String showSubDirectoryOfDirectory(@P("The directory to show all direct sub-directories for. Must be relative to the root of the project (e.g., 'directoryInRoot/subDirectory')") String directory) throws IOException {
        if (directory.contains("..")) {
            return "Error, not allowed to look outside of the project.";
        }
        Path resolved = root.resolve(directory);
        if (Files.notExists(resolved)) {
            return "Error, the provided directory does not exist. Make sure to provide a directory relative to the root directory of the project.";
        }

        List<String> results = new LinkedList<>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(resolved)) {
            for (Path path : directoryStream) {
                if (Files.isDirectory(path)) {
                    results.add(root.relativize(path).toString().replace("\\", "/"));
                    try (DirectoryStream<Path> subDirectoryStream = Files.newDirectoryStream(path)) {
                        for (Path subPath : subDirectoryStream) {
                            if (Files.isDirectory(subPath)) {
                                results.add(root.relativize(subPath).toString().replace("\\", "/"));
                            }
                        }
                    }
                }
            }
        }
        
        return results.isEmpty() ? "No results." : Jsons.writeValueAsString(results);
    }

    @Tool("Returns a summary of all file extensions contained in this directory.")
    public String fileExtensionsSummary(@P("The directory to retrieve the summary of contained file types for. Must be relative to the root of the project (e.g., 'directoryInRoot/subDirectory')") String directory) throws IOException {
        if (directory.contains("..")) {
            return "Error, not allowed to look outside of the project.";
        }
        Map<String, Integer> fileExtensionMapping = new TreeMap<>();
        Path resolved = root.resolve(directory);
        if (Files.notExists(resolved)) {
            return "Error, the provided directory does not exist. Make sure to provide a directory relative to the root directory of the project.";
        }
        try (DirectoryStream<Path> walk = Files.newDirectoryStream(resolved)) {
            for (Path path : walk) {
                if (Files.isRegularFile(path)) {
                    String fileExtension = getFileExtension(path);
                    fileExtensionMapping.putIfAbsent(fileExtension, 0);
                    fileExtensionMapping.compute(fileExtension, (ignored, count) -> ++count);
                }
            }
//                walk.filter(Files::isRegularFile).map(ProjectTool::getFileExtension).forEach(fileExtension -> {
//                    fileExtensionMapping.putIfAbsent(fileExtension, 0);
//                    fileExtensionMapping.compute(fileExtension, (ignored, count) -> ++count);
//                });
        }
        Map<Integer, List<String>> fileExtensionCount = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<String, Integer> entry : fileExtensionMapping.entrySet()) {
            fileExtensionCount.putIfAbsent(entry.getValue(), new ArrayList<>());
            fileExtensionCount.get(entry.getValue()).add(entry.getKey());
        }
        
        Map<String, Integer> results = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<String>> entry : fileExtensionCount.entrySet()) {
            for (String fileExtension : entry.getValue()) {
                results.put(fileExtension, entry.getKey());
            }
        }
        
        return results.isEmpty() ? "No results." : Jsons.writeValueAsString(results);
    }

    @Tool("Returns a list of all sub-directories that contain files ending with the specified file extension.")
    public String fileExtensionDirectories(@P("The directory to start looking for file extensions. Must be relative to the root of the project (e.g., 'directoryInRoot/subDirectory')") String directory
            , @P("The file extension to look for, e.g., '.properties.java'") String fileExtension) throws IOException {
        if (directory.contains("..")) {
            return "Error, not allowed to look outside of the project.";
        }
        Path resolved = root.resolve(directory);
        if (Files.notExists(resolved)) {
            return "Error, the provided directory does not exist. Make sure to provide a directory relative to the root directory of the project.";
        }
        try (Stream<Path> walk = Files.walk(resolved)) {
            TreeSet<String> results = walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(fileExtension))
                    .map(root::relativize)
                    .map(Path::getParent)
                    .map(path -> "- " + (path == null ? "./" : path.toString()).replace("\\", "/"))
                    .collect(Collectors.toCollection(TreeSet::new));
            return results.isEmpty() ? "No results." : Jsons.writeValueAsString(results);
        }
    }

    public static String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.indexOf(".");
        return fileName.substring(dotIndex == -1 ? 0 : dotIndex);
    }

    @Tool("Validates whether the given directory actually exists in the project.")
    public boolean validateDirectory(@P("The directory to validate.") String directory) {
        return !directory.contains("..") && Files.exists(root.resolve(directory));
    }
}
