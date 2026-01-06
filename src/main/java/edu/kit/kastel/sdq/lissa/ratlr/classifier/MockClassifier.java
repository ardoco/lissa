/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.Optional;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * A mock classifier implementation that always returns a positive classification result.
 * This classifier is primarily used for testing and demonstration purposes.
 */
public class MockClassifier extends Classifier {

    /**
     * The identifier used for this classifier type in configuration and caching.
     */
    public static final String MOCK_CLASSIFIER_NAME = "mock";

    /**
     * Creates a new mock classifier instance.
     * The classifier uses a single thread for processing.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public MockClassifier(ContextStore contextStore) {
        super(1, contextStore);
    }

    /**
     * Always classifies any pair of elements as related with maximum confidence.
     * This method is used for testing and demonstration purposes.
     *
     * @param source The source element
     * @param target The target element
     * @return A classification result with maximum confidence (1.0)
     */
    @Override
    protected Optional<ClassificationResult> classify(Element source, Element target) {
        return Optional.of(ClassificationResult.of(source, target, 1.0));
    }

    @Override
    public Classifier copyOf() {
        return new MockClassifier(contextStore);
    }

    @Override
    public void setClassificationPrompt(String prompt) {
        // TODO: This may / should throw an UnsportedOperationException instead?
        // as no classification prompt is used, this method does nothing
    }

    @Override
    public String getClassificationPromptKey() {
        throw new UnsupportedOperationException(
                "MockClassifier does not support retrieving a single classification prompt key.");
    }
}
