package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ElementFormatter implements ValueFormatter<Element> {

    private final AtomicReference<Element> elementReference = new AtomicReference<>();
    private final TemplateFormatter formatter;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param configuration The module configuration containing the placeholder format and template
     * @param contextStore The shared context store for pipeline components
     */
    public ElementFormatter(ModuleConfiguration configuration, ContextStore contextStore) {
        ReplacementRetriever retriever = new ContextReplacementRetriever(new ElementReplacementRetriever(null, elementReference), contextStore);
        this.formatter = new TemplateFormatter(configuration, retriever);
    }
    public ElementFormatter(ModuleConfiguration configuration, ContextStore contextStore, String templateKey) {
        ReplacementRetriever retriever = new ContextReplacementRetriever(new ElementReplacementRetriever(null, elementReference), contextStore);
        this.formatter = new TemplateFormatter(configuration, retriever, templateKey);
    }
    
    @Override
    public void setValue(Element value) {
        elementReference.set(value);
    }

    @Override
    public String format() {
        return formatter.format();
    }

    @Override
    public void addRetriever(Function<ReplacementRetriever, ReplacementRetriever> retrieverProvider) {
        formatter.addRetriever(retrieverProvider);
    }
}
