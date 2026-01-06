/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Holds the result of a single evaluation instance.
 * @param source the source element
 * @param target the target element
 * @param groundTruth the ground truth classification
 * @param classification the classification result
 * @param <T> the type of the classification labels
 */
public record EvaluationResult<T>(Element source, Element target, T groundTruth, T classification) {

    public String getTextualRepresentation() {
        return String.format("Source: %s, Target: %s,", source.getContent(), target.getContent());
    }

    /**
     * Checks if the classification matches the ground truth.
     * @return true if the classification is equal to the ground truth, false otherwise
     */
    public boolean isIncorrect() {
        return !groundTruth.equals(classification);
    }

    @Override
    public String toString() {
        return String.format(
                "Source: %s, Target: %s, Ground Truth Label: %s, Classification Label: %s",
                source.getIdentifier(), target.getIdentifier(), groundTruth, classification);
    }
}
