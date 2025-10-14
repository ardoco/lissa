package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentNames;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.CosineSimilarity;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy.RetrievalStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator.EmbeddingCreator;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SentenceComponents extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT =
            "Your task is to analyse software architecture documentation. Each sentence of the documentation is prefixed with its identifier. Extract for each sentence which component of the project it describes.";
    private static final String USER_MESSAGE_FORMAT = "%s";
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"ComponentExtraction\",\"description\": \"A sentence from the document with additional instructions on how to evaluate this sentence\",\"type\": \"object\",\"properties\": {\"documentation\": {\"description\": \"The software architecture documentation of the project.\",\"type\": \"array\",\"items\": {\"$ref\": \"#/$defs/sentence\"}}},\"$defs\": {\"sentence\": {\"type\": \"object\",\"properties\": {\"id\": {\"description\": \"The identifier of the sentence.\",\"type\": \"integer\"},\"content\": {\"description\": \"The verbatim content of the sentence.\",\"type\": \"string\"},\"components\": {\"description\": \"The names of the components that this sentence describes. They exclude prefixes and suffixes that are not essential.\",\"type\": \"array\",\"items\": {\"type\": \"string\"},\"minItems\": 0}},\"required\": [\"id\", \"content\", \"components\"]}},\"required\": [\"documentation\"]}";
    private final EmbeddingCreator embeddingCreator;
    private final RetrievalStrategy retrievalStrategy = new CosineSimilarity(1);


    public SentenceComponents(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
        Map<String, String> embeddingArguments = new HashMap<>();
        embeddingArguments.put("model", configuration.argumentAsString("embedding_model", "text-embedding-3-large"));
        this.embeddingCreator = EmbeddingCreator.createEmbeddingCreator(
                new ModuleConfiguration(configuration.argumentAsString("embedding_platform"), embeddingArguments), contextStore);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        return List.of(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        contextStore.createContext(new StringContext("sentence_components", responses.getFirst()));
        ComponentExtraction extraction = Jsons.readValue(responses.getFirst(), new TypeReference<>() {});
        
        List<Element> results = new LinkedList<>();
        Element responseElement = Element.fromParent(elements.getFirst(), 0, responses.getFirst(), false);
        results.add(responseElement);

        Map<String, String> componentNameMapping = getComponentNameMapping(extraction);
        Element nameMappingElement = Element.fromParent(responseElement, 0, Jsons.writeValueAsString(componentNameMapping), false);
        results.add(nameMappingElement);

        for (Sentence sentence : extraction.documentation) {
            sentence.components = sentence.components.stream().map(componentNameMapping::get).toList();
            results.add(Element.fromParent(nameMappingElement, sentence.id - 1, Jsons.writeValueAsString(sentence), true));
        }
        
        return results;
    }

    private Map<String, String> getComponentNameMapping(ComponentExtraction extraction) {
        SortedSet<String> componentNames = new TreeSet<>();
        for (Sentence sentence : extraction.documentation) {
            componentNames.addAll(sentence.components);
        }

        List<Pair<Element, float[]>> sentenceComponentNames = componentNames.stream()
                .map(name -> new Element("sentence_component_name", "ignored", name, 0, null, false))
                .map(element -> new Pair<>(element, embeddingCreator.calculateEmbedding(element)))
                .toList();
        List<Pair<Element, float[]>> simpleComponentNames = contextStore.getContext("documentation_component_names", ComponentNames.class).getNames().stream()
                .map(name -> new Element("simple_component_name", "ignored", name, 0, null, false))
                .map(element -> new Pair<>(element, embeddingCreator.calculateEmbedding(element)))
                .toList();

        return sentenceComponentNames.stream()
                .collect(Collectors.toMap(pair -> pair.first().getContent(),
                        pair -> retrievalStrategy.findSimilarElements(pair, simpleComponentNames).getFirst().first().getContent()));
    }

    private static final class ComponentExtraction {
        @JsonProperty
        private List<Sentence> documentation;
    }
    
    private static final class Sentence {
        @JsonProperty
        private int id;
        @JsonProperty
        private String content;
        @JsonProperty
        private List<String> components;
    }
}
