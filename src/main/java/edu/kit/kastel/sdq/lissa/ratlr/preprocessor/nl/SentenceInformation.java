package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.nl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.LanguageModelPreprocessor;

import java.util.ArrayList;
import java.util.List;

/**
 * A preprocessor that extracts sentences and corresponding information from a text using a language model.
 * This preprocessor is part of the "sentence" type in the preprocessor hierarchy.
 * It:
 * <ul>
 *     <li>Directly uses the artifact content as a user message</li>
 *     <li>Creates elements with granularity level 1</li>
 *     <li>Marks all elements for comparison (compare=true)</li>
 * </ul>
 *
 * Configuration options:
 * <ul>
 *     <li>content_template: The template string for formatting the content of the resulting elements</li>
 * </ul>
 *
 * The template for the content can use the following placeholders:
 * <ul>
 *     <li>{sentence} - An extracted sentence from the text</li>
 *     <li>{information} - Additional information regarding the sentence</li>
 * </ul>
 *
 * @see LanguageModelPreprocessor
 */
public class SentenceInformation extends LanguageModelPreprocessor {
    
    private final String contentTemplate;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore        The shared context store for pipeline components
     */
    public SentenceInformation(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
        super(moduleConfiguration, contextStore);
        this.contentTemplate = moduleConfiguration.argumentAsString("content_template", """
                Sentence of documentation: {sentence}
                Identification task: {information}""");
    }

    @Override
    protected String createRequest(Element element) {
        return element.getContent();
    }

    @Override
    protected List<Element> createElements(Element element, String response) {
        String[] sentenceElements = response.split("\n");
        List<Element> elements = new ArrayList<>(sentenceElements.length + 1);
        
        for (int i = 0; i < sentenceElements.length; i++) {
            String sentenceElement = sentenceElements[i].replace("- ", "");
            SentenceElement extractedElement;
            try {
                extractedElement = new ObjectMapper().readValue(sentenceElement, SentenceElement.class);
            } catch (JsonProcessingException e) {
                logger.error("Artifact {}: Invalid json format for sentence element '{}'", element.getIdentifier(), sentenceElement);
                throw new RuntimeException(e);
            }

            for (String task : extractedElement.identificationTasks) {
                // TODO check if identifier must be unique, otherwise adapt postprocessing
                elements.add(Element.fromParent(element
                        , i
                        , contentTemplate.replace("{sentence}", extractedElement.sentence).replace("{information}", task)
                        , true));
            }
            
        }
        return elements;
    }
    
    private static final class SentenceElement {
        @JsonProperty("sentence")
        private String sentence;
        @JsonProperty("identification_tasks")
        private List<String> identificationTasks;
    }
}
