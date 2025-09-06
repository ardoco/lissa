package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.json;

import com.fasterxml.jackson.databind.JsonNode;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text.TemplateReplacer;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

public class JsonConverterText extends TemplateReplacer {

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonConverterText(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
    }

    @Override
    protected String retrieveReplacement(Element element, String placeholderKey) {
        String replacement = super.retrieveReplacement(element, placeholderKey);
        if (replacement != null) {
            return replacement;
        }

        JsonNode rootNode = Jsons.readTree(element.getContent());
        if (rootNode.has(placeholderKey)) {
            JsonNode jsonNode = rootNode.get(placeholderKey);
            return jsonNode.isTextual()
                    ? jsonNode.textValue()
                    : jsonNode.toString();
        }
        
        return null;
    }
}
