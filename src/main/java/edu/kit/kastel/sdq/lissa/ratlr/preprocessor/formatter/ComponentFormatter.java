package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;

import java.util.concurrent.atomic.AtomicReference;

public class ComponentFormatter {

    private final AtomicReference<Component> componentReference = new AtomicReference<>();
    private final TemplateFormatter formatter;
    
    public ComponentFormatter(ModuleConfiguration configuration, ContextStore contextStore) {
        this.formatter = new TemplateFormatter(configuration, new ContextReplacementRetriever(new ComponentReplacementRetriever(null, componentReference), contextStore));
    }
    
    public void setComponent(Component component) {
        componentReference.set(component);
    }
    
    public String format() {
        return formatter.format();
    }
}
