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

    private static final String SENTENCE_SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "title": "Extraction",
                "description": "A sentence from the document with additional instructions on how to evaluate this sentence",
                "type": "object",
                "properties": {
                    "documentation": {
                        "description": "The software architecture documentation of the project.",
                        "type": "array",
                        "items": {
                            "$ref": "#/$defs/sentence"
                        }
                    }
                },
                "$defs": {
                    "sentence": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "description": "The identifier of the sentence.",
                                "type": "integer"
                            },
                            "content": {
                                "description": "The verbatim content of the sentence.",
                                "type": "string"
                            },
                            "components": {
                                "description": "The components that this sentence describes.",
                                "type": "array",
                                "items": {
                                    "type": "string"
                                },
                                "minItems": 0
                            }
                        },
                        "required": ["id", "content", "components"]
                    }
                },
                "required": ["documentation"]
            }""";

    private static final String SECTIONS_SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "title": "SectionExtraction",
                "description": "Extraction of sections of a software architecture documentation.",
                "type": "object",
                "properties": {
                    "sections": {
                        "description": "The extracted sections of the software architecture documentation.",
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "title": {
                                    "description": "The title of the section.",
                                    "type": "string"
                                },
                                "sentences": {
                                    "description": "The sentences that correspond to the section, including their identifier.",
                                    "type": "array",
                                    "items": {
                                        "type": "string"
                                    }
                                },
                                "summary": {
                                    "description": "A summary of what the sections describes or its purpose in the documentation.",
                                    "type": "string"
                                }
                            },
                            "required": ["title", "sentences", "summary"]
                        }
                    }
                },
                "required": ["sections"]
            }""";

    private static final String SECTIONS_SCHEMA_LINE = """
            {"$schema": "https://json-schema.org/draft/2020-12/schema","title": "SectionExtraction","description": "Extraction of sections of a software architecture documentation.","type": "object","properties": {"sections": {"description": "The extracted sections of the software architecture documentation.","type": "array","items": {"type": "object","properties": {"title": {"description": "The title of the section.","type": "string"},"sentences": {"description": "The sentences that correspond to the section, including their identifier.","type": "array","items": {"type": "string"}},"summary": {"description": "A summary of what the sections describes or its purpose in the documentation.","type": "string"}},"required": ["title", "sentences", "summary"]}}},"required": ["sections"]}""";

    private static final String SENTENCE_CODE_OBJECTS_SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "title": "Extraction",
                "description": "Extraction of mentioned code objects to be retrieved in the project with information about them.",
                "type": "object",
                "properties": {
                    "documentation": {
                        "description": "The software architecture documentation of the project.",
                        "type": "array",
                        "items": {
                            "$ref": "#/$defs/sentence"
                        }
                    }
                },
                "$defs": {
                    "sentence": {
                        "type": "object",
                        "properties": {
                            "id": {
                                "description": "The identifier of the sentence.",
                                "type": "integer"
                            },
                            "content": {
                                "description": "The verbatim content of the sentence.",
                                "type": "string"
                            },
                            "mentionedCodebaseObjects": {
                                "description": "Objects in the codebase that are mentioned in the sentence.",
                                "type": "array",
                                "items": {
                                    "type": "object",
                                    "description": "A mentioned code object with its name and information about it.",
                                    "name": {
                                        "type": "string",
                                        "description": "The name of the mentioned code object."
                                    },
                                    "information": {
                                        "type": "string",
                                        "description": "Information about the code object."
                                    },
                                    "required": ["name", "information"]
                                }
                            }
                        },
                        "required": ["id", "content", "mentionedCodebaseObjects"]
                    }
                },
                "required": ["documentation"]
            }""";

    private static final String SENTENCE_CODE_OBJECTS_SCHEMA_LINE = """
            {"$schema": "https://json-schema.org/draft/2020-12/schema","title": "Extraction","description": "Extraction of mentioned code objects to be retrieved in the project with information about them.","type": "object","properties": {"documentation": {"description": "The software architecture documentation of the project.","type": "array","items": {"$ref": "#/$defs/sentence"}}},"$defs": {"sentence": {"type": "object","properties": {"id": {"description": "The identifier of the sentence.","type": "integer"},"content": {"description": "The verbatim content of the sentence.","type": "string"},"mentionedCodebaseObjects": {"description": "Objects in the codebase that are mentioned in the sentence.","type": "array","items": {"type": "object","description": "A mentioned code object with its name and information about it.","name": {"type": "string","description": "The name of the mentioned code object."},"information": {"type": "string","description": "Information about the code object."},"required": ["name", "information"]}}},"required": ["id", "content", "mentionedCodebaseObjects"]}},"required": ["documentation"]}""";

    private static final String SENTENCE_RETRIEVAL_SCHEMA_LINE = "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"Extraction\",\"description\": \"A sentence from the document with additional instructions on how to evaluate this sentence\",\"type\": \"object\",\"properties\": {\"documentation\": {\"description\": \"The software architecture documentation of the project.\",\"type\": \"array\",\"items\": {\"$ref\": \"#/$defs/sentence\"}}},\"$defs\": {\"sentence\": {\"type\": \"object\",\"properties\": {\"id\": {\"description\": \"The identifier of the sentence.\",\"type\": \"integer\"},\"content\": {\"description\": \"The verbatim content of the sentence.\",\"type\": \"string\"},\"components\": {\"description\": \"The components that this sentence describes.\",\"type\": \"array\",\"items\": {\"type\": \"string\"},\"minItems\": 0},\"retrieve\": {\"description\": \"Specifications for tasks to retrieve files in the project that the sentence describes.\",\"type\": \"array\",\"items\": {\"$ref\": \"#/$defs/retrieval\"}}},\"required\": [\"id\", \"content\", \"components\", \"retrieve\"]},\"retrieval\": {\"description\": \"Information about how to retrieve certain files in the project.\",\"type\": \"object\",\"properties\": {\"method\": {\"description\": \"The type of the retrieval method to be used.\",\"enum\": [\"COMPONENT\", \"PACKAGE\", \"FILE\", \"NOTHING\"]},\"target\": {\"description\": \"The target to be retrieved.\",\"type\": \"string\"},\"additionalInformation\": {\"description\": \"Additional information to consider when retrieving the target with the retrieval method.\",\"type\": \"string\"}},\"required\": [\"method\", \"target\", \"additionalInformation\"]}},\"required\": [\"documentation\"]}";

    private static final String SENTENCE_SCHEMA_LINE = "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"Extraction\",\"description\": \"A sentence from the document with additional instructions on how to evaluate this sentence\",\"type\": \"object\",\"properties\": {\"documentation\": {\"description\": \"The software architecture documentation of the project.\"\"type\": \"array\",\"items\": {\"$ref\": \"#/$defs/sentence\"}}},\"$defs\": {\"sentence\": {\"type\": \"object\",\"properties\": {\"id\": {\"description\": \"The identifier of the sentence.\",\"type\": \"integer\"},\"content\": {\"description\": \"The verbatim content of the sentence.\",\"type\": \"string\"},\"components\": {\"description\": \"The components that this sentence describes.\",\"type\": \"array\",\"items\": {\"type\": \"string\"},\"minItems\": 0}},\"required\": [\"id\", \"content\", \"components\"]}},\"required\": [\"documentation\"]}";

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
