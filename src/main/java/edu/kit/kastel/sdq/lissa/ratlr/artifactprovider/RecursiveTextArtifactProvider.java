/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

/**
 * Provides text-based artifacts from a directory structure recursively.
 * This provider reads text files from a directory and its subdirectories, using the relative path
 * as the artifact identifier. Artifacts represent the original documents that will be processed
 * into elements by preprocessors. It supports filtering files by their extensions.
 *
 * Configuration parameters:
 * <ul>
 * <li>path: The path to the root directory containing the artifacts</li>
 * <li>artifact_type: The type of artifact to create (e.g., REQUIREMENT, SOURCE_CODE)</li>
 * <li>extensions: Comma-separated list of file extensions to consider (e.g., "txt,java,md")</li>
 * </ul>
 */
public class RecursiveTextArtifactProvider extends TextArtifactProvider {

    /**
     * Array of file extensions to consider when loading artifacts.
     * Extensions are stored in lowercase for case-insensitive matching.
     */
    private final String[] extensions;

    /**
     * Creates a new recursive text artifact provider with the specified configuration.
     *
     * @param configuration The configuration containing the path, artifact type, and file extensions
     * @param contextStore The shared context store for pipeline components
     * @throws IllegalArgumentException If the specified path does not exist
     */
    public RecursiveTextArtifactProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        this.extensions =
                configuration.argumentAsString("extensions").toLowerCase().split(",");
    }

    public RecursiveTextArtifactProvider(Artifact.ArtifactType artifactType, String path, String[] extensions, ContextStore contextStore) {
        super(artifactType, path, contextStore);
        this.extensions = Arrays.copyOf(extensions, extensions.length);
    }

    /**
     * Recursively loads text files from the configured directory and its subdirectories.
     * Only files with the specified extensions are processed. The relative path of each file
     * is used as the artifact identifier. These artifacts will later be processed into elements
     * by preprocessors.
     *
     * @throws UncheckedIOException If there are issues reading the files
     */
    @Override
    protected void loadFiles() {
        try (Stream<Path> fileStream = Files.walk(this.path.toPath())) {
            for (Path file : fileStream.toList()) {
                if (Files.isRegularFile(file) && hasCorrectExtension(file)) {
                    readFile(file);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Reads a single file and creates an artifact from its contents.
     * The artifact's identifier is set to the relative path of the file,
     * with path separators normalized to forward slashes. This artifact will later
     * be processed into elements by preprocessors.
     *
     * @param file The path to the file to read
     * @throws UncheckedIOException If there are issues reading the file
     */
    private void readFile(Path file) {
        try (Scanner scan = new Scanner(file.toFile()).useDelimiter("\\A")) {
            if (scan.hasNext()) {
                String content = scan.next();
                var relativePath = this.path.toPath().relativize(file);
                String pathWithDefinedSeparators = relativePath.toString().replace("\\", "/");
                artifacts.add(new Artifact(pathWithDefinedSeparators, artifactType, content));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Checks if a file has one of the configured extensions.
     * The check is case-insensitive.
     *
     * @param file The path to the file to check
     * @return true if the file has a matching extension, false otherwise
     */
    private boolean hasCorrectExtension(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        for (String extension : extensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
