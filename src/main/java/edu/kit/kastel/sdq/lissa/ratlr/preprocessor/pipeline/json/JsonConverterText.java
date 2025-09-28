package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;

public class JsonConverterText extends TemplateElement {

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonConverterText(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
    }
}
