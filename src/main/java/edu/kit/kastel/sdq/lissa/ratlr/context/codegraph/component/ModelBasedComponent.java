package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import spoon.reflect.CtModel;

import java.util.Collection;
import java.util.SortedSet;

public class ModelBasedComponent extends PathBasedComponent {

    private final Collection<CtModel> models;

    public ModelBasedComponent(String simpleName, String qualifiedName, SortedSet<String> paths, Collection<CtModel> models) {
        super(simpleName, qualifiedName, paths);
        this.models = models;
    }
}
