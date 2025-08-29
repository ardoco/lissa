/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.List;

/**
 * A preprocessor that generates summaries of artifacts using a language model.
 * This preprocessor is part of the "summarize" type in the preprocessor hierarchy.
 * It:
 * <ul>
 *     <li>Uses a configurable template to format summary requests</li>
 *     <li>Supports parallel processing with multiple threads</li>
 *     <li>Caches summaries to avoid redundant processing</li>
 *     <li>Creates elements with granularity level 0</li>
 *     <li>Marks all elements for comparison (compare=true)</li>
 * </ul>
 *
 * The template for summary requests can use the following placeholders:
 * <ul>
 *     <li>{type} - The type of the artifact (e.g., "source code", "requirement")</li>
 *     <li>{content} - The content of the artifact to be summarized</li>
 * </ul>
 *
 * Configuration options:
 * <ul>
 *     <li>template: The template string for formatting summary requests</li>
 *     <li>model: The language model to use for summarization</li>
 *     <li>seed: Random seed for reproducible results</li>
 * </ul>
 *
 */
public class SummarizePreprocessor extends LanguageModelPreprocessor {
    /** The template string for formatting summary requests */
    private final String template;

    /**
     * Creates a new summarize preprocessor with the specified configuration and context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore The shared context store for pipeline components
     */
    public SummarizePreprocessor(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
        super(moduleConfiguration, contextStore);
        this.template = moduleConfiguration.argumentAsString("template", "Summarize the following {type}: {content}");
    }

    @Override
    protected String createRequest(Element element) {
        return template.replace("{type}", element.getType()).replace("{content}", element.getContent());
    }

    @Override
    protected List<Element> createElements(Element element, String response) {
        return List.of(new Element(
                element.getIdentifier(),
                "Summary of '%s'".formatted(element.getType()),
                response,
                0,
                null,
                true));
    }
}





















