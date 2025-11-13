/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a detailed result of a classification decision,
 * including category (TP/FP/FN/TN) and associated source/target elements.
 * This class is serializable to and from JSON for persistence.
 */
public class DetailedClassificationResult {

    /**
     * Classification outcome category (TP, FP, FN, TN).
     * Serialized as a string via Jackson.
     */
    @JsonProperty("category")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Category category;
    /**
     * Identifier of the source element.
     */
    @JsonProperty("SourceId")
    private String sourceId;
    /**
     * Content of the source element.
     */
    @JsonProperty("SourceContent")
    private String sourceContent;
    /**
     * Identifier of the target element.
     */
    @JsonProperty("TargetId")
    private String targetId;
    /**
     * Content of the target element.
     */
    @JsonProperty("TargetContent")
    private String targetContent;

    /**
     * Default constructor required for JSON deserialization.
     */
    public DetailedClassificationResult() {
    }

    /**
     * Creates a new classification result.
     *
     * @param category      classification category
     * @param sourceId      source element identifier
     * @param sourceContent source element content
     * @param targetId      target element identifier
     * @param targetContent target element content
     */
    public DetailedClassificationResult(Category category, String sourceId, String sourceContent,
                                        String targetId, String targetContent) {
        this.category = category;
        this.sourceId = sourceId;
        this.sourceContent = sourceContent;
        this.targetId = targetId;
        this.targetContent = targetContent;
    }

    /**
     * @return the classification category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the source element identifier
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @return the source element content
     */
    public String getSourceContent() {
        return sourceContent;
    }

    /**
     * @return the target element identifier
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * @return the target element content
     */
    public String getTargetContent() {
        return targetContent;
    }

    /**
     * Classification outcome category: True-Positive, False-Positive, True-Negative, False-Negative
     */
    public enum Category {TP, FP, TN, FN}
}
