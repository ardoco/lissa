package edu.kit.kastel.sdq.lissa.ratlr.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.context.Context;
import spoon.reflect.CtModel;

public class ModelContext implements Context {

    private final String identifier;
    private final CtModel model;

    public ModelContext(String identifier, CtModel model) {
        this.identifier = identifier;
        this.model = model;
    }

    public CtModel getModel() {
        return model;
    }

    @Override
    public String getId() {
        return identifier;
    }
}
