package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.AmbiguityMapper;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class SentenceComponentsResolver extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            You'll be given a sentence from a documentation that describes the components of a software project.
            Your task is to make sure whether the sentence **truly** describes the additionally provided component that has been extracted before.
            Use information about ambiguities, that is provided as well, to justify your reasoning.
            
            1. Explain whether the sentence actually describes the provided component of the project.
            2. Then give your final decision. If you decide that the sentence actually describes the provided component of the project, then choose the provided component.
            Otherwise, if your explanation suggest another component that rather should have been extracted, then choose this component.
            Otherwise, choose `-`.
            """;
    private static final String USER_MESSAGE_FORMAT = """
            The full documentation containing all sentences:
            ```
            %s
            ```
            
            The sentence: `%s`
            
            The extracted component: `%s`
            
            Additional information about ambiguities regarding this and other components:
            ```
            %s
            ```
            """;
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"ExtractionEvaluation\",\"description\": \"Evaluation whether the extracted component is actually being describes by the sentence.\",\"type\": \"object\",\"properties\": {\"explanation\": {\"description\": \"The explanation whether the extracted component is actually being describes by the sentence.\",\"type\": \"string\"},\"finalDecision\": {\"description\": \"The final decision whether the extracted component is actually being describes by the sentence.\",\"enum\": [<<<context_documentation_component_names_json>>>, \"-\"]}},\"required\": [\"explanation\", \"finalDecision\"]}";


    public SentenceComponentsResolver(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        AmbiguityMapper ambiguityMapper = contextStore.getContext("component_ambiguity_mapper", AmbiguityMapper.class);
        List<String> requests = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            Sentence sentence = Jsons.readValue(element.getContent(), new TypeReference<>() {});
            if (ambiguityMapper.isAmbiguous(sentence.component)) {
                Map<SortedSet<String>, String> sharedAmbiguities = ambiguityMapper.getSharedAmbiguities(sentence.component);
                String ambiguityInformation = sharedAmbiguities.entrySet().stream()
                        .map(entry -> "Ambiguities of `%s` with %s: %s"
                                .formatted(sentence.component, "`" + String.join("`, `", entry.getKey()) + "`", entry.getValue()))
                        .collect(Collectors.joining("\n"));

                requests.add(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()
                        , sentence.id + ": " + sentence.content, sentence.component, ambiguityInformation));
            } else {
                requests.add(null);
            }
        }
        return requests;
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        List<Element> results = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            if (responses.get(i) == null) {
                results.add(Element.fromParent(elements.get(i), elements.get(i).getContent()));
                continue;
            }
            
            ExtractionEvaluation evaluation = Jsons.readValue(responses.get(i), new TypeReference<>() {});
            if (evaluation.finalDecision.equals(elements.get(i).getContent())) {
                results.add(Element.fromParent(elements.get(i), elements.get(i).getContent()));
            } else if (evaluation.finalDecision.equals("-")) {
                results.add(Element.fromParent(elements.get(i), i, evaluation.explanation, false));
            } else {
                results.add(Element.fromParent(elements.get(i), evaluation.finalDecision));
            }
        }
        return results;
    }

    private static final class Sentence {
        @JsonProperty
        private int id;
        @JsonProperty
        private String content;
        @JsonProperty
        private String component;
    }
    
    private static final class ExtractionEvaluation {
        @JsonProperty
        private String explanation;
        @JsonProperty
        private String finalDecision;
    }
}
