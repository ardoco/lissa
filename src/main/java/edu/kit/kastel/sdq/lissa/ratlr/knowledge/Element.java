/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.knowledge;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

// TODO consider moving separator to here
import static edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor.SEPARATOR;

/**
 * Represents an element in the LiSSA framework, which is a granular unit of knowledge
 * that can be traced and compared. Elements form a hierarchical structure through
 * parent references, where each element knows its parent but not its children.
 *
 * Elements are organized in a hierarchical structure where:
 * <ul>
 *     <li>Each element can have a parent element, creating a tree-like structure</li>
 *     <li>Elements maintain a reference to their parent, but not to their children</li>
 *     <li>The granularity level indicates the element's position in this hierarchy,
 *         with higher numbers representing more detailed/granular elements</li>
 *     <li>Parent-child relationships are maintained through parent references and
 *         parent identifiers for serialization</li>
 * </ul>
 *
 * Each element contains:
 * <ul>
 *     <li>An identifier that uniquely identifies the element</li>
 *     <li>A type that categorizes the element</li>
 *     <li>Content that represents the actual text or data of the element</li>
 *     <li>A granularity level indicating the element's level of detail</li>
 *     <li>An optional parent element, creating a hierarchical structure</li>
 *     <li>A comparison flag indicating whether the element should be included in comparisons</li>
 * </ul>
 *
 * The class supports JSON serialization and deserialization through Jackson annotations,
 * making it suitable for storage and transmission of element data. During deserialization,
 * the parent-child relationships are restored through the {@link #init(Map)} method,
 * which reconnects elements using their identifiers.
 */
public final class Element extends Knowledge {
    /** The granularity level of this element, indicating its level of detail */
    @JsonProperty
    private final int granularity;

    /** The parent element of this element, if any */
    @JsonIgnore
    private Element parent;

    /** The identifier of the parent element, used for JSON serialization */
    @JsonProperty
    private final String parentId;

    /**
     *  Flag indicating whether this element should be included in comparisons.
     *  {@link Classifier Classifiers} will only consider this element for candidate pairs for classification if this is true.
     */
    @JsonProperty
    private boolean compare;

    /**
     * Creates a new element from JSON data.
     * This constructor is used by Jackson for deserialization.
     *
     * @param identifier The unique identifier of the element
     * @param type The type of the element
     * @param content The content of the element
     * @param granularity The granularity level of the element
     * @param parentId The identifier of the parent element, if any
     * @param compare Whether the element should be included in comparisons
     */
    @JsonCreator
    private Element(
            @JsonProperty("identifier") String identifier,
            @JsonProperty("type") String type,
            @JsonProperty("content") String content,
            @JsonProperty("granularity") int granularity,
            @JsonProperty("parentId") String parentId,
            @JsonProperty("compare") boolean compare) {
        super(identifier, type, content);
        this.granularity = granularity;
        this.parentId = parentId;
        this.compare = compare;
    }

    /**
     * Creates a new element with the specified properties.
     *
     * @param identifier The unique identifier of the element
     * @param type The type of the element
     * @param content The content of the element
     * @param granularity The granularity level of the element
     * @param parent The parent element, if any
     * @param compare Whether the element should be included in comparisons
     */
    public Element(String identifier, String type, String content, int granularity, Element parent, boolean compare) {
        super(identifier, type, content);
        this.granularity = granularity;
        this.parentId = parent == null ? null : parent.getIdentifier();
        this.parent = parent;
        this.compare = compare;
    }

    /**
     * Initializes the parent-child relationships for this element.
     * This method is called after deserialization to restore the parent references
     * using the parent identifiers.
     *
     * @param otherKnowledge A map of all available elements, indexed by their identifiers
     */
    public void init(Map<String, Element> otherKnowledge) {
        if (parentId != null) {
            parent = otherKnowledge.get(parentId);
        }
    }

    /**
     * Gets the granularity level of this element.
     *
     * @return The granularity level
     */
    public int getGranularity() {
        return granularity;
    }

    /**
     * Gets the parent element of this element.
     *
     * @return The parent element, or null if this element has no parent
     */
    public Element getParent() {
        return parent;
    }

    /**
     * Checks whether this element should be included in comparisons.
     *
     * @return true if the element should be compared, false otherwise
     */
    public boolean isCompare() {
        return compare;
    }

    /**
     * Sets whether this element should be included in the classification.
     * 
     * @param compare true if the element should be classified, false otherwise
     */
    public void setCompare(boolean compare) {
        this.compare = compare;
    }

    /**
     * Convenience method to create a new element as a child of a parent element.
     * The parent identifier is appended by a {@link Preprocessor#SEPARATOR separator} followed by the identifier index.
     * The type is taken from the parent.
     * The granularity is incremented by one.
     * 
     * @param parent the parent to set and retrieve information from
     * @param identifierIndex the index of this child used in the identifier
     * @param content the content of this child
     * @param compare whether this child should be included in the classification
     * @return a new child element based on the parent
     */
    public static Element fromParent(Element parent, int identifierIndex, String content, boolean compare) {
        return new Element(parent.getIdentifier() + SEPARATOR + identifierIndex
                , parent.getType()
                , content
                , parent.getGranularity() + 1
                , parent
                , compare);
    }

    /**
     * Convenience method to create a single child based on a parent.
     * The identifier index is set to 0.
     * The child is flagged to be included for classification.
     *
     * @param parent the parent to set and retrieve information from
     * @param content the content of this child
     * @return a new child element based on the parent
     * @see #fromParent(Element, int, String, boolean) 
     */
    public static Element fromParent(Element parent, String content) {
        return fromParent(parent, 0, content, true);
    }
}
