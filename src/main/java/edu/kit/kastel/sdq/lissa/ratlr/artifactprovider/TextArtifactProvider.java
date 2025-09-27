/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.io.IOUtils;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Knowledge;

/**
 * Provides text-based artifacts from a configured file or directory.
 * This provider reads text files and creates artifacts using the filename as the identifier.
 * Artifacts represent the original documents that will be processed into elements by preprocessors.
 * It supports both single files and directories containing multiple text files.
 *
 * Configuration parameters:
 * <ul>
 * <li>path: The path to the file or directory containing text files</li>
 * <li>artifact_type: The type of artifact to create (e.g., REQUIREMENT, SOURCE_CODE)</li>
 * </ul>
 */
public class TextArtifactProvider extends PathedProvider {

    /**
     * The type of artifacts to be created.
     */
    protected final Artifact.ArtifactType artifactType;

    /**
     * Cache of loaded artifacts.
     */
    protected final List<Artifact> artifacts;

    /**
     * Creates a new text artifact provider with the specified configuration.
     *
     * @param configuration The configuration containing the path and artifact type
     * @param contextStore The shared context store for pipeline components
     * @throws IllegalArgumentException If the specified path does not exist
     */
    public TextArtifactProvider(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        this.artifactType = Artifact.ArtifactType.from(configuration.argumentAsString("artifact_type"));
        this.artifacts = new ArrayList<>();
    }
    
    protected TextArtifactProvider(Artifact.ArtifactType artifactType, String path, ContextStore contextStore) {
        super(path, contextStore);
        this.artifactType = artifactType;
        this.artifacts = new ArrayList<>();
    }

    /**
     * Loads text files from the configured path and creates artifacts.
     * If the path is a file, it creates a single artifact.
     * If the path is a directory, it creates artifacts for all text files in the directory.
     * These artifacts will later be processed into elements by preprocessors.
     *
     * @throws UncheckedIOException If there are issues reading the files
     */
    protected void loadFiles() {
        List<File> files = new ArrayList<>();
        if (this.path.isFile()) {
            files.add(this.path);
        } else {
            files.addAll(Arrays.asList(Objects.requireNonNull(this.path.listFiles())));
        }

        for (File file : files) {
            Path it = file.toPath();
            if (Files.isRegularFile(it)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (BufferedReader reader = new BufferedReader(new FileReader(it.toFile()))) {
                    IOUtils.copy(reader, bos, StandardCharsets.UTF_8);
                    String content = bos.toString(StandardCharsets.UTF_8);
                    artifacts.add(new Artifact(it.getFileName().toString(), artifactType, content));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    /**
     * Retrieves all artifacts from the configured path.
     * The artifacts are loaded if they haven't been loaded yet, and returned in alphabetical order
     * by their identifiers. These artifacts represent the original documents that will be
     * processed into elements by preprocessors.
     *
     * @return A sorted list of all artifacts
     */
    @Override
    public List<Artifact> getArtifacts() {
        if (artifacts.isEmpty()) this.loadFiles();
        var orderedArtifacts = new ArrayList<>(this.artifacts);
        orderedArtifacts.sort(Comparator.comparing(Knowledge::getIdentifier));
        return orderedArtifacts;
    }

    /**
     * Retrieves a specific artifact by its identifier.
     * The artifacts are loaded if they haven't been loaded yet.
     *
     * @param identifier The filename of the artifact to retrieve
     * @return The artifact with the specified identifier
     * @throws IllegalArgumentException If no artifact with the given identifier exists
     */
    @Override
    public Artifact getArtifact(String identifier) {
        if (artifacts.isEmpty()) this.loadFiles();
        return artifacts.stream()
                .filter(it -> it.getIdentifier().equals(identifier))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found"));
    }
}
