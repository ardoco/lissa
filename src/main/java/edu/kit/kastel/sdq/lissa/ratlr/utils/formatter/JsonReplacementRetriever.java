package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import com.fasterxml.jackson.databind.JsonNode;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.concurrent.atomic.AtomicReference;

public class JsonReplacementRetriever extends SupplyingRetriever<String> {

    public JsonReplacementRetriever(ReplacementRetriever formatter, AtomicReference<String> valueReference) {
        super(formatter, valueReference);
    }

    @Override
    public String retrieveReplacement(String value, String placeholderKey) {
        JsonNode rootNode = Jsons.readTree(value);
        if (rootNode.has(placeholderKey)) {
            JsonNode jsonNode = rootNode.get(placeholderKey);
            return jsonNode.isTextual()
                    ? jsonNode.textValue()
                    : jsonNode.toString();
        }

        return null;
    }
}
