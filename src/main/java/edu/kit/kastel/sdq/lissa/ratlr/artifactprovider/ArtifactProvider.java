/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.artifactprovider;

import java.util.List;
import java.util.Objects;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;

/**
 * Abstract base class for artifact providers in the LiSSA framework.
 * This class defines the interface for classes that provide artifacts (source or target documents)
 * for trace link analysis. Artifacts are the original documents (like requirements or source code files)
 * that are later processed into elements by preprocessors.
 * <p>
 * All artifact providers have access to a shared {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} via the protected {@code contextStore} field,
 * which is initialized in the constructor and available to all subclasses.
 * Subclasses should not duplicate context handling.
 * </p>
 * Different implementations can provide artifacts from various sources such as text files,
 * directories, or other data sources.
 */
public abstract class ArtifactProvider {

    /**
     * The shared context store for pipeline components.
     * Available to all subclasses for accessing shared context.
     */
    protected final ContextStore contextStore;

    /**
     * Creates a new artifact provider with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected ArtifactProvider(ContextStore contextStore) {
        this.contextStore = Objects.requireNonNull(contextStore);
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

    /**
     * Creates an appropriate artifact provider based on the given configuration.
     * The factory method supports different types of artifact providers:
     * - "text": Provides artifacts from individual text files
     * - "recursive_text": Provides artifacts from text files in a directory structure
     *
     * @param configuration The configuration specifying the type and parameters of the artifact provider
     * @param contextStore The shared context store for pipeline components
     * @return An instance of the appropriate artifact provider
     * @throws IllegalStateException If the configuration specifies an unsupported provider type
     */
    public static ArtifactProvider createArtifactProvider(
            ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name()) {
            case "text" -> new TextArtifactProvider(configuration, contextStore);
            case "recursive_text" -> new RecursiveTextArtifactProvider(configuration, contextStore);
            case "code" -> new CodeGraphProvider(configuration, contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
