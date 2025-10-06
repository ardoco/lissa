/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.knowledge;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents an artifact in the LiSSA framework, which is a high-level unit of knowledge
 * that can contain multiple elements. Artifacts are typically used to represent
 * different types of software artifacts like source code, documentation, or models.
 * <p>
 * Artifacts serve as containers for elements, where:
 * <ul>
 *     <li>Each artifact represents a complete software artifact (e.g., a source file,
 *         a requirements document, or an architecture model)</li>
 *     <li>Elements within an artifact represent more granular parts (e.g., methods,
 *         requirements, or model components)</li>
 *     <li>The artifact type categorizes the kind of software artifact it represents,
 *         which helps in organizing and analyzing trace links</li>
 * </ul>
 *
 * The class supports two types of construction:
 * <ul>
 *     <li>Using an {@link ArtifactType} enum value, which provides type safety and
 *         ensures only valid artifact types are used</li>
 *     <li>Using a string type, which is useful for JSON deserialization and allows
 *         for flexible type handling</li>
 * </ul>
 *
 * Artifacts are used as containers for elements, where elements represent more
 * granular units of knowledge within the artifact. This hierarchical organization
 * allows for both high-level artifact analysis and detailed element-level tracing.
 */
public final class Artifact extends Knowledge {

    /**
     * Creates a new artifact with the specified properties using an ArtifactType.
     * The type is converted to a lowercase string with spaces for storage.
     *
     * @param identifier The unique identifier of the artifact
     * @param type The type of the artifact as an enum value
     * @param content The content of the artifact
     */
    public Artifact(String identifier, ArtifactType type, String content) {
        super(identifier, type.toString().replace("_", " ").toLowerCase(), content);
    }

    /**
     * Creates a new artifact with the specified properties using a string type.
     * This constructor is primarily used for JSON deserialization.
     *
     * @param identifier The unique identifier of the artifact
     * @param type The type of the artifact as a string
     * @param content The content of the artifact
     */
    @JsonCreator
    public Artifact(String identifier, String type, String content) {
        super(identifier, type, content);
    }

    /**
     * Enumeration of supported artifact types in the LiSSA framework.
     * Each type represents a different category of software artifact that can be
     * analyzed for trace links.
     */
    public enum ArtifactType {
        /** Source code files or code snippets */
        SOURCE_CODE,
        /** Documentation describing software architecture */
        SOFTWARE_ARCHITECTURE_DOCUMENTATION,
        /** Requirements specifications */
        REQUIREMENT,
        /** Models representing software architecture */
        SOFTWARE_ARCHITECTURE_MODEL;

        /**
         * Converts a string representation of an artifact type to the corresponding enum value.
         * The string can be in either format:
         * <ul>
         *     <li>Space-separated lowercase (e.g., "source code")</li>
         *     <li>Underscore-separated uppercase (e.g., "SOURCE_CODE")</li>
         * </ul>
         *
         * @param type The string representation of the artifact type
         * @return The corresponding ArtifactType enum value
         * @throws IllegalArgumentException If the string does not match any known artifact type
         */
        public static ArtifactType from(String type) {
            for (ArtifactType artifactType : values()) {
                if (artifactType.toString().equalsIgnoreCase(type.replace(" ", "_"))) {
                    return artifactType;
                }
            }
            throw new IllegalArgumentException("Unknown artifact type '" + type + "'");
        }
    }
}
