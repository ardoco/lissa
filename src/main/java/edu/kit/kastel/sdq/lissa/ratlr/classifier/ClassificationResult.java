/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Represents the result of a trace link classification between two elements.
 * This record stores the source and target elements involved in the classification,
 * along with a confidence score indicating the strength of the relationship.
 * @param source The source element in the trace link relationship
 * @param target The target element in the trace link relationship
 * @param confidence The confidence score of the classification, ranging from 0.0 to 1.0
 *                   A score of 1.0 indicates maximum confidence in the relationship
 */
public record ClassificationResult(Element source, Element target, double confidence) {
    /**
     * Validates the confidence score during record construction.
     *
     * @throws IllegalArgumentException If the confidence score is not between 0 and 1
     */
    public ClassificationResult {
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
    }

    /**
     * Creates a classification result with maximum confidence (1.0).
     *
     * @param source The source element
     * @param target The target element
     * @return A new classification result with maximum confidence
     */
    public static ClassificationResult of(Element source, Element target) {
        return new ClassificationResult(source, target, 1.0);
    }

    /**
     * Creates a classification result with the specified confidence score.
     *
     * @param source The source element
     * @param target The target element
     * @param confidence The confidence score (must be between 0 and 1)
     * @return A new classification result with the specified confidence
     * @throws IllegalArgumentException If the confidence score is not between 0 and 1
     */
    public static ClassificationResult of(Element source, Element target, double confidence) {
        return new ClassificationResult(source, target, confidence);
    }
}
