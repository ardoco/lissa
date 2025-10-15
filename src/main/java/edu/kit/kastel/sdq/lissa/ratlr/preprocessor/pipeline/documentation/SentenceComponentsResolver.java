package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonCreator;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SentenceComponentsResolver extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            You'll be given a sentence from a documentation that describes the components of a software project.
            Your task is to make sure whether the extracted component **truly** is expected to contain the code that is described by the sentence.
            Use information about ambiguities, that is provided as well, to justify your reasoning.
            
            1. Explain whether the extracted component actually is expected to contain what is described by the sentence.
            2. Then give your final decision.
            """;
    private static final String USER_MESSAGE_FORMAT = """
            The full documentation containing all sentences:
            ```
            %s
            ```
            
            The sentence: `%s`
            
            The extracted component: `%s`
            
            Other extracted components for this sentence: %s
            
            Additional information about ambiguities regarding this and other components:
            ```
            %s
            ```
            """;
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"ExtractionEvaluation\",\"description\": \"Evaluation whether the extracted component is expected to contain what is described by the sentence.\",\"type\": \"object\",\"properties\": {\"explanation\": {\"description\": \"The explanation whether the extracted component is expected to contain what is described by the sentence.\",\"type\": \"string\"},\"finalDecision\": {\"description\": \"The final decision whether the extracted component is expected to contain what is described by the sentence.\",\"type\": \"boolean\"}},\"required\": [\"explanation\", \"finalDecision\"]}";
    private final Map<Element, List<String>> componentsByElement = new LinkedHashMap<>();
    private final Map<Element, List<String>> populatedComponentsByElement = new LinkedHashMap<>();
    private final List<Element> results = new LinkedList<>();

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
            SortedSet<String> populatedAmbiguityComponents = sentence.components.stream()
                    .flatMap(component -> ambiguityMapper.getSharedAmbiguities(component).keySet().stream().flatMap(SortedSet::stream))
                    .collect(Collectors.toCollection(TreeSet::new));
            populatedAmbiguityComponents.addAll(sentence.components);
            results.add(Element.fromParent(element, i,
                    Jsons.writeValueAsString(new Sentence(sentence.id, sentence.content, populatedAmbiguityComponents.stream().toList())), 
                    false));
            componentsByElement.put(element, new ArrayList<>(populatedAmbiguityComponents.size()));
            populatedComponentsByElement.put(element, populatedAmbiguityComponents.stream().toList());
            for (String component : populatedAmbiguityComponents) {
                if (ambiguityMapper.isAmbiguous(component)) {
                    Map<SortedSet<String>, String> sharedAmbiguities = ambiguityMapper.getSharedAmbiguities(component);
                    StringJoiner ambiguityInformation = new StringJoiner("\n");
                    for (Map.Entry<SortedSet<String>, String> entry : sharedAmbiguities.entrySet()) {
                        String formatted = "Ambiguities of `%s` with %s: %s"
                                .formatted(component, "`" + String.join("`, `", entry.getKey()) + "`", entry.getValue());
                        ambiguityInformation.add(formatted);
                    }
                    registerRequestJsonSchema(requests.size(), JSON_SCHEMA_TEMPLATE_DEFAULT);

                    requests.add(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()
                            , sentence.id + ": " + sentence.content
                            , component
                            , sentence.components.stream().filter(Predicate.not(component::equals)).collect(Collectors.joining("`, `", "`", "`"))
                            , ambiguityInformation));
                    componentsByElement.get(element).add(null);
                } else {
                    componentsByElement.get(element).add(component);
                }
            }
        }
        return requests;
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        int requestId = 0;
        for (Map.Entry<Element, List<String>> entry : componentsByElement.entrySet()) {
            Element element = entry.getKey();
            List<String> value = entry.getValue();
            for (int i = 0; i < value.size(); i++) {
                String component = value.get(i);
                if (component != null) {
                    results.add(Element.fromParent(element, i, component, true));
                    continue;
                }

                Element responseElement = Element.fromParent(element, i, responses.get(requestId++), false);
                results.add(responseElement);
                ExtractionEvaluation evaluation = Jsons.readValue(responseElement.getContent(), new TypeReference<>() {});
                if (evaluation.finalDecision) {
                    results.add(Element.fromParent(responseElement, populatedComponentsByElement.get(element).get(i)));
                }
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
        private List<String> components;

        @JsonCreator
        public Sentence(@JsonProperty("id") int id, @JsonProperty("content") String content, @JsonProperty("components") List<String> components) {
            this.id = id;
            this.content = content;
            this.components = components;
        }
    }
    
    private static final class ExtractionEvaluation {
        @JsonProperty
        private String explanation;
        @JsonProperty
        private boolean finalDecision;
    }
}
