package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.CodeObjectsTool;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.PipelinePreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CodeObjectsWriter extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT =
            "Your task is to analyse software architecture documentation. Each sentence of the documentation is prefixed with its identifier. Extract for each sentence the objects in the codebase it describes and collect information about them.";
    private static final String USER_MESSAGE_FORMAT = "%s";
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"Extraction\",\"description\": \"Extraction of mentioned code objects to be retrieved in the project with information about them.\",\"type\": \"object\",\"properties\": {\"documentation\": {\"description\": \"The software architecture documentation of the project.\",\"type\": \"array\",\"items\": {\"$ref\": \"#/$defs/sentence\"}}},\"$defs\": {\"sentence\": {\"type\": \"object\",\"properties\": {\"id\": {\"description\": \"The identifier of the sentence.\",\"type\": \"integer\"},\"content\": {\"description\": \"The verbatim content of the sentence.\",\"type\": \"string\"},\"mentionedCodebaseObjects\": {\"description\": \"Objects in the codebase that are mentioned in the sentence.\",\"type\": \"array\",\"items\": {\"type\": \"object\",\"description\": \"A mentioned code object with its name and information about it.\",\"name\": {\"type\": \"string\",\"description\": \"The name of the mentioned code object.\"},\"information\": {\"type\": \"string\",\"description\": \"Information about the code object.\"},\"required\": [\"name\", \"information\"]}}},\"required\": [\"id\", \"content\", \"mentionedCodebaseObjects\"]}},\"required\": [\"documentation\"]}";

    public CodeObjectsWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        return List.of(USER_MESSAGE_FORMAT.formatted(PipelinePreprocessor.getLineIdPrefixedDocumentation(contextStore)));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        Map<String, List<String>> codeObjectInformation = new TreeMap<>();
        Extraction extraction = Jsons.readValue(responses.getFirst(), new TypeReference<>() {});
        for (Sentence sentence : extraction.documentation) {
            for (CodebaseObject codebaseObject : sentence.mentionedCodebaseObjects) {
                codeObjectInformation.putIfAbsent(codebaseObject.name, new ArrayList<>());
                codeObjectInformation.get(codebaseObject.name).add(codebaseObject.information);
            }
        }
        contextStore.createContext(new CodeObjectsTool("code_object_information_map", codeObjectInformation));
        contextStore.createContext(new StringContext("code_object_names", Jsons.writeValueAsString(codeObjectInformation.keySet())));
        elements.forEach(element -> element.setCompare(true));
        return elements;
    }
    
    private static final class Extraction {
        @JsonProperty
        private List<Sentence> documentation;
    }
    
    private static final class Sentence {
        @JsonProperty
        private int id;
        @JsonProperty
        private String content;
        @JsonProperty
        private List<CodebaseObject> mentionedCodebaseObjects;
    }
    
    private static final class CodebaseObject {
        @JsonProperty
        private String name;
        @JsonProperty
        private String information;
    }
}
