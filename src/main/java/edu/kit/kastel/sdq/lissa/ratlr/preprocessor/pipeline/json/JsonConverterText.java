package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ContextReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ElementReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.JsonReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class JsonConverterText extends TemplateElement {

    private final AtomicReference<String> jsonReference = new AtomicReference<>();
    private final AtomicReference<Element> jsonRetrieverElementReference = new AtomicReference<>();
    private final ReplacementRetriever jsonRetriever;
    private final String jsonRetrieverKey;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonConverterText(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        getFormatter().getReplacer().addRetriever(templateElementRetriever -> new JsonReplacementRetriever(templateElementRetriever, jsonReference));
        jsonRetriever = new ContextReplacementRetriever(new ElementReplacementRetriever(null, jsonRetrieverElementReference), contextStore);
        jsonRetrieverKey = configuration.argumentAsString("json_source", "element_content");
    }

    @Override
    public List<Element> process(Element element) {
        jsonRetrieverElementReference.set(element);
        jsonReference.set(jsonRetriever.retrieveReplacement(jsonRetrieverKey));
        return super.process(element);
    }
}
