package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
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
 * @see LanguageModelRequester
 */
public class SentenceInformationPreprocessor extends Preprocessor {

    private final SentenceInformation requestProcessor;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected SentenceInformationPreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.requestProcessor = new SentenceInformation(configuration, contextStore);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        List<Element> elements = new LinkedList<>();
        for (Artifact artifact : artifacts) {
            elements.add(new Element(artifact.getIdentifier(), artifact.getType(), artifact.getContent(), 0, null, false));
        }
        elements.addAll(requestProcessor.process(elements));
        return elements;
    }

    private static final class SentenceInformation extends LanguageModelRequester {

        private final String contentTemplate;

        /**
         * Creates a new preprocessor with the specified context store.
         *
         * @param moduleConfiguration The module configuration containing template and model settings
         * @param contextStore        The shared context store for pipeline components
         */
        private SentenceInformation(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
            super(moduleConfiguration, contextStore);
            this.contentTemplate = moduleConfiguration.argumentAsString("content_template", """
                Sentence of documentation: {sentence}
                Identification task: {information}""");
        }

        @Override
        protected List<String> createRequests(List<Element> elements) {
            return elements.stream().map(Element::getContent).toList();
        }

        @Override
        protected List<Element> createElements(List<Element> elements, List<String> responses) {
            List<Element> result = new LinkedList<>();
            for (int i = 0; i < elements.size(); i++) {
                result.addAll(createElements(elements.get(i), responses.get(i)));
            }
            return result;
        }

        @NotNull
        private List<Element> createElements(Element element, String response) {
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
    }

    private static final class SentenceElement {
        @JsonProperty("sentence")
        private String sentence;
        @JsonProperty("identification_tasks")
        private List<String> identificationTasks;
    }
}
