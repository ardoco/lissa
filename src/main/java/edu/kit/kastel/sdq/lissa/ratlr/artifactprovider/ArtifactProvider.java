/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

import java.util.List;

/**
 * Abstract base class for artifact providers in the LiSSA framework.
 * This class defines the interface for classes that provide artifacts (source or target documents)
 * for trace link analysis. Artifacts are the original documents (like requirements or source code files)
 * that are later processed into elements by preprocessors.
 * <p>
 * Different implementations can provide artifacts from various sources such as text files,
 * directories, or other data sources.
 */
public abstract class ArtifactProvider {

    /**
     * Creates an appropriate artifact provider based on the given configuration.
     * The factory method supports different types of artifact providers:
     * - "text": Provides artifacts from individual text files
     * - "recursive_text": Provides artifacts from text files in a directory structure
     *
     * @param configuration The configuration specifying the type and parameters of the artifact provider
     * @return An instance of the appropriate artifact provider
     * @throws IllegalStateException If the configuration specifies an unsupported provider type
     */
    public static ArtifactProvider createArtifactProvider(ModuleConfiguration configuration) {
        return switch (configuration.name()) {
            case "text" -> new TextArtifactProvider(configuration);
            case "recursive_text" -> new RecursiveTextArtifactProvider(configuration);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    /**
     * Retrieves all artifacts provided by this provider.
     * Artifacts represent the original documents that will be processed into elements
     * by the preprocessing step.
     *
     * @return A list of artifacts provided by this provider
     */
    public abstract List<Artifact> getArtifacts();

    /**
     * Retrieves a specific artifact by its identifier.
     * The implementation should return the artifact that matches the given identifier,
     * or null if no such artifact exists.
     *
     * @param identifier The unique identifier of the artifact to retrieve
     * @return The artifact with the specified identifier, or null if not found
     */
    public abstract Artifact getArtifact(String identifier);
}
