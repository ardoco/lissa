/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Represents a classification task for trace link prediction between two elements.
 * Each task consists of a source element, a target element, and a label indicating
 * whether a trace link exists between them.
 *
 * @param source The source element in the trace link relationship.
 * @param target The target element in the trace link relationship.
 * @param label  The label indicating whether a trace link exists between the source and target elements.
 */
public record ClassificationTask(Element source, Element target, boolean label)
        implements Comparable<ClassificationTask> {
    @Override
    public String toString() {
        return "ClassificationTask{" + "source="
                + source.getIdentifier() + ", target="
                + target.getIdentifier() + ", label="
                + label + '}';
    }

    /**
     * Compares this classification task to another based on source and target element identifiers and label.
     * <ul>
     *     <li>this == other if both source IDs, target IDs, and labels are equal.</li>
     *     <li>this &lt; other if:
     *     <ul>
     *         <li>source ID is less, or</li>
     *         <li>source IDs are equal and target ID is less, or</li>
     *         <li>both IDs are equal and this.label is false while other.label is true. </li>
     *     </ul></li>
     *     <li>this &gt; other otherwise.</li>
     * </ul>
     *
     * @param other The other classification task to compare to (must not be null).
     * @return A negative integer, zero, or a positive integer as this task is less than,
     *         equal to, or greater than the specified task.
     * @throws NullPointerException if other is null
     */
    @Override
    public int compareTo(ClassificationTask other) {
        int sourceComparison = this.source.getIdentifier().compareTo(other.source.getIdentifier());
        if (sourceComparison != 0) {
            return sourceComparison;
        }
        int targetComparison = this.target.getIdentifier().compareTo(other.target.getIdentifier());
        if (targetComparison != 0) {
            return targetComparison;
        }
        return Boolean.compare(this.label, other.label);
    }
}
