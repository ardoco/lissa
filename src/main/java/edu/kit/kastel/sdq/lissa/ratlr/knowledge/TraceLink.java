/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.knowledge;

/**
 * Represents a trace link between two {@link Element Elements} in the LiSSA framework.
 * A trace link establishes a relationship between a source element and a target element,
 * indicating a connection or dependency between them.
 *
 * @param sourceId The unique identifier of the source element
 * @param targetId The unique identifier of the target element
 */
public record TraceLink(String sourceId, String targetId) implements Comparable<TraceLink> {
    /**
     * Compares this trace link to another based on source and target identifiers.
     * <ul>
     *     <li>this == other if both source IDs and target IDs are equal.</li>
     *     <li>this &lt; other if source ID is less, or if source IDs are equal and target ID is less.</li>
     *     <li>this &gt; other otherwise.</li>
     * </ul>
     *
     * @param other The other trace link to compare to (must not be null).
     * @return A negative integer, zero, or a positive integer as this trace link is less than,
     *         equal to, or greater than the specified trace link.
     * @throws NullPointerException if other is null
     */
    @Override
    public int compareTo(TraceLink other) {
        int sourceComparison = this.sourceId.compareTo(other.sourceId);
        return sourceComparison != 0 ? sourceComparison : this.targetId.compareTo(other.targetId);
    }

    public static TraceLink of(String sourceId, String targetId) {
        return new TraceLink(sourceId, targetId);
    }
}
