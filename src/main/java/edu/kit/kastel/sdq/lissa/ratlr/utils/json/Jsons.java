package edu.kit.kastel.sdq.lissa.ratlr.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Jsons {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private Jsons() throws IllegalAccessException {
        throw new IllegalAccessException("utility class");
    }
    
    public static <R, A> R visit(JsonNodeBaseVisitor<R, A> visitor, String json, A arg) throws JsonProcessingException {
        JsonNode root = new ObjectMapper().readTree(json);
        return visitor.visit(root, arg);
    }


    public static JsonNode readTree(String content) {
        return readTree(content, MAPPER);
    }

    /**
     * Convenience method to deserialize JSON content.
     * 
     * @param content the JSON content to get deserialized
     * @return the deserialized JSON content
     * @see ObjectMapper#readTree(String)
     */
    public static JsonNode readTree(String content, ObjectMapper mapper) {
        try {
            return mapper.readTree(content);
        } catch (JsonProcessingException e) {
            // TODO log error
            throw new RuntimeException(e);
        }
    }


    public static <T> String writeValueAsString(T value) {
        return writeValueAsString(value, MAPPER);
    }

    /**
     * Convenience method to serialize a value.
     * 
     * @param value the value to get serialized
     * @return the serialized JSON
     * @see ObjectMapper#writeValueAsString(Object)
     */
    public static <T> String writeValueAsString(T value, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            // TODO log error
            throw new RuntimeException(e);
        }
    }
    
    public static <T> T readValue(String json, TypeReference<T> typeReference) {
        return readValue(json, typeReference, MAPPER);
    }
    
    public static <T> T readValue(String json, TypeReference<T> typeReference, ObjectMapper mapper) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            // TODO log error
            throw new RuntimeException(e);
        }
    }
}
