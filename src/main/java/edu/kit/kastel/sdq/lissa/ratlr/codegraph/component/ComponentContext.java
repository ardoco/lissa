package edu.kit.kastel.sdq.lissa.ratlr.codegraph.component;

import edu.kit.kastel.sdq.lissa.ratlr.context.Context;

import java.util.Collection;
import java.util.Collections;

public class ComponentContext implements Context {
    
    private final String identifier;
    private final Collection<Component> component;

    public ComponentContext(String identifier, Collection<Component> component) {
        this.identifier = identifier;
        this.component = component;
    }

    public Collection<Component> getComponent() {
        return Collections.unmodifiableCollection(component);
    }

    @Override
    public String getId() {
        return identifier;
    }
}
