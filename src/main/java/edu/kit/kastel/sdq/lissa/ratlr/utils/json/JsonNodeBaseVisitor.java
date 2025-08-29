package edu.kit.kastel.sdq.lissa.ratlr.utils.json;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeBaseVisitor<R, A> implements NodeVisitor<R, A> {

    @Override
    public R visitArray(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitBinary(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitBoolean(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitMissing(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitNull(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitNumber(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitObject(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitPojo(JsonNode node, A arg) {
        return null;
    }

    @Override
    public R visitString(JsonNode node, A arg) {
        return null;
    }

    public final R visit(JsonNode node, A arg) {
        return switch (node.getNodeType()) {
            case ARRAY -> visitArray(node, arg);
            case BINARY -> visitBinary(node, arg);
            case BOOLEAN -> visitBoolean(node, arg);
            case MISSING -> visitMissing(node, arg);
            case NULL -> visitNull(node, arg);
            case NUMBER -> visitNumber(node, arg);
            case OBJECT -> visitObject(node, arg);
            case POJO -> visitPojo(node, arg);
            case STRING -> visitString(node, arg);
        };
    }
    
}
