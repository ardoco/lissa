package edu.kit.kastel.sdq.lissa.ratlr.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

public interface NodeVisitor<R, A> {

    R visitArray(JsonNode node, A arg);
    R visitBinary(JsonNode node, A arg);
    R visitBoolean(JsonNode node, A arg);
    R visitMissing(JsonNode node, A arg);
    R visitNull(JsonNode node, A arg);
    R visitNumber(JsonNode node, A arg);
    R visitObject(JsonNode node, A arg);
    R visitPojo(JsonNode node, A arg);
    R visitString(JsonNode node, A arg);
}
