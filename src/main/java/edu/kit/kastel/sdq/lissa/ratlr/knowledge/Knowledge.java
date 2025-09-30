/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Abstract base class representing a unit of knowledge in the LiSSA framework.
 * This class serves as the foundation for different types of knowledge units,
 * specifically artifacts and elements, which are used in trace link analysis.
 *
 * The class is designed as a sealed class, meaning it can only be extended by
 * the explicitly permitted classes: {@link Artifact} and {@link Element}. This
 * design ensures that all knowledge units in the system are either artifacts
 * (high-level containers) or elements (granular units), providing a clear and
 * controlled hierarchy of knowledge representation.
 *
 * Each knowledge unit contains:
 * <ul>
 *     <li>An identifier that uniquely identifies the knowledge unit</li>
 *     <li>A type that categorizes the knowledge unit (e.g., requirement)</li>
 *     <li>Content that represents the actual text or data</li>
 *     <li>A normalized version of the content with consistent line endings</li>
 * </ul>
 *
 * The class supports JSON serialization and deserialization through Jackson annotations,
 * with type information included in the JSON to allow proper reconstruction of the
 * concrete class (Artifact or Element) during deserialization. The type information
 * is stored in a "type" field, which is used to determine the correct subclass to
 * instantiate when deserializing JSON data.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Artifact.class, name = "artifact"),
    @JsonSubTypes.Type(value = Element.class, name = "element")
})
public abstract sealed class Knowledge permits Artifact, Element {
    /** The unique identifier of this knowledge unit */
    @JsonProperty
    private final String identifier;

    /** The type of this knowledge unit */
    @JsonProperty
    private final String type;

    /** The original content of this knowledge unit */
    @JsonProperty
    private final String content;

    /** The normalized content with consistent line endings */
    @JsonIgnore
    private final String normalizedContent;

    /**
     * Creates a new knowledge unit with the specified properties.
     * The content is automatically normalized to use consistent line endings.
     *
     * @param identifier The unique identifier of the knowledge unit
     * @param type The type of the knowledge unit
     * @param content The content of the knowledge unit
     */
    protected Knowledge(String identifier, String type, String content) {
        this.identifier = identifier;
        this.type = type;
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("content must not be null or empty");
        }
        this.content = content;
        this.normalizedContent = content.replace("\r\n", "\n");
    }

    /**
     * Gets the normalized content of this knowledge unit.
     * The content is normalized to use consistent line endings (Unix-style).
     *
     * @return The normalized content
     */
    public final String getContent() {
        // We do want to return the normalized content here
        return normalizedContent;
    }

    /**
     * Gets the unique identifier of this knowledge unit.
     *
     * @return The identifier
     */
    public final String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the type of this knowledge unit.
     * For artifacts, the type will be one of the values from {@link Artifact.ArtifactType}
     * (e.g., "source code", "software architecture documentation", "requirement", "software architecture model").
     * For elements, the type will be "element".
     *
     * @return The type of this knowledge unit
     */
    public final String getType() {
        return type;
    }
}
