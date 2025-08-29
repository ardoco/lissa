/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * A preprocessor that treats each artifact as a single element without any further processing.
 * This preprocessor is the simplest implementation in the preprocessor hierarchy, as it:
 * <ul>
 *     <li>Does not break down artifacts into smaller pieces</li>
 *     <li>Preserves the original artifact's content and type</li>
 *     <li>Creates elements with granularity level 0</li>
 *     <li>Marks all elements for comparison (compare=true)</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class SingleArtifactPreprocessor extends Preprocessor<Artifact> {

    public SingleArtifactPreprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    /**
     * Preprocesses a list of artifacts by converting each one into a single element.
     * This method:
     * <ol>
     *     <li>Takes each artifact as is</li>
     *     <li>Creates a new element with the same identifier, type, and content</li>
     *     <li>Sets granularity level to 0 (no hierarchy)</li>
     *     <li>Marks the element for comparison</li>
     * </ol>
     *
     * @param artifacts The list of artifacts to preprocess
     * @return A list of elements, one for each input artifact
     */
    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        return artifacts.stream()
                .map(artifact ->
                        new Element(artifact.getIdentifier(), artifact.getType(), artifact.getContent(), 0, null, true))
                .toList();
    }
}
