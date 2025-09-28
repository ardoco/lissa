package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ComponentFormatter implements ValueFormatter<Component> {

    private final AtomicReference<Component> componentReference = new AtomicReference<>();
    private final TemplateFormatter formatter;
    
    public ComponentFormatter(ModuleConfiguration configuration, ContextStore contextStore) {
        this.formatter = new TemplateFormatter(configuration, new ContextReplacementRetriever(new ComponentReplacementRetriever(null, componentReference), contextStore));
    }
    
    @Override
    public void setValue(Component component) {
        componentReference.set(component);
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
