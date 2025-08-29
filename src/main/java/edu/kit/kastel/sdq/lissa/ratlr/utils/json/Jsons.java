package edu.kit.kastel.sdq.lissa.ratlr.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Jsons {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    
    private Jsons() throws IllegalAccessException {
        throw new IllegalAccessException("utility class");
    }
    
    public static <R, A> R visit(JsonNodeBaseVisitor<R, A> visitor, String json, A arg) throws JsonProcessingException {
        JsonNode root = new ObjectMapper().readTree(json);
        return visitor.visit(root, arg);
    }

    /**
     * Convenience method to deserialize JSON content.
     * 
     * @param content the JSON content to get deserialized
     * @return the deserialized JSON content
     * @see ObjectMapper#readTree(String)
     */
    public static JsonNode readTree(String content) {
        try {
            return MAPPER.readTree(content);
        } catch (JsonProcessingException e) {
            // TODO log error
            throw new RuntimeException(e);
        }
    }
}
