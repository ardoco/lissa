package edu.kit.kastel.sdq.lissa.ratlr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonParser {

    private static final String SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "title": "Extraction",
                "description": "A sentence from the document with additional instructions on how to evaluate this sentence",
                "type": "object",
                "properties": {
                    "sentence": {
                        "description": "Verbatim sentence from the document",
                        "type": "string"
                    },
                    "identification_tasks": {
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "minItems": 1
                    }
                },
                "required": ["sentence", "identification_tasks"]
            }""";

    private static final String SCHEMA_IN_LINE = "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"Extraction\",\"description\": \"A sentence from the document with additional instructions on how to evaluate this sentence\",\"type\": \"object\",\"properties\": {\"sentence\": {\"description\": \"Verbatim sentence from the document\",\"type\": \"string\"},\"identification_tasks\": {\"type\": \"array\",\"items\": {\"type\": \"string\"},\"minItems\": 1}},\"required\": [\"sentence\", \"identification_tasks\"]}";

    private static final String RESPONSE = """
            {
                "sentence": "The system uses a pipeline architecture.",
                "identification_tasks": [
                    "Identification of the class or method that defines the pipeline-like behavior."
                ]
            }""";
    private static final String MULTIPLE_TASKS = """
            {
                "sentence": "Each component works independently, only accepting the results of the previous stage, where their specifics are configurable.",
                "identification_tasks": [
                    "Identify main components representing the pipeline, by identifying their constructors accepting the previous result and configuration.",
                    "Identify the configuration that makes the components configurable."
                ],
                "end": "endValue",
                "otherArray": [
                    "insideOtherArray",
                    "whatever"
                ],
                "thirdArray": [
                    "insideThird",
                    "secondOfThird",
                    "thirdOfThird"
                ]
            }""";
    
    @Test
    public void onJsonTest() throws JsonProcessingException {
        Map<String, String> remapper = new HashMap<>();
        remapper.put("identification_tasks", "identification_task");
        remapper.put("otherArray", "other");
        remapper.put("thirdArray", "third");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, JsonNode> children = mapper.readValue(MULTIPLE_TASKS, new TypeReference<Map<String, JsonNode>>() {});
        List<Map<String, String>> results = new LinkedList<>();
        results.add(new LinkedHashMap<>());
        for (Map.Entry<String, JsonNode> childEntry : children.entrySet()) {
            if (remapper.containsKey(childEntry.getKey()) && childEntry.getValue().getNodeType().equals(JsonNodeType.ARRAY)) {                
                List<Map<String, String>> newResults = new LinkedList<>();
                for (JsonNode arrayElement : childEntry.getValue()) {
                    for (Map<String, String> resultWithoutArrayElement : results) {
                        Map<String, String> newResult = new LinkedHashMap<>(resultWithoutArrayElement);
                        newResult.put(remapper.get(childEntry.getKey()), mapper.writeValueAsString(arrayElement));
                        newResults.add(newResult);
                    }
                }
                results = newResults;
            } else {
                for (Map<String, String> result : results) {
                    result.put(childEntry.getKey(), mapper.writeValueAsString(childEntry.getValue()));
                }
            }
        }
        for (Map<String, String> result : results) {
            System.out.println(result);
        }
    }
}
