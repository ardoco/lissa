package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.ModelContext;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.component.ComponentContext;
import edu.kit.kastel.sdq.lissa.ratlr.codegraph.component.Components;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider.CONTEXT_PREFIX;

public class ComponentPreprocessor extends CodeGraphPreprocessor {
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public ComponentPreprocessor(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        contextStore.createContext(new StringContext(CONTEXT_PREFIX + "retriever", "component"));
        CtModel model = contextStore.getContext(CodeGraphProvider.CONTEXT_MODEL, ModelContext.class).getModel();
        Collection<Component> components = Components.getComponents(model);
        contextStore.createContext(new ComponentContext(CONTEXT_PREFIX + "components", components));

        List<Element> elements = new ArrayList<>(components.size());
        for (Component component : components) {
            CtPackage rootPackage = component.getRootPackage();
            elements.add(new Element(rootPackage.getSimpleName(), "source code component name", rootPackage.getQualifiedName(), 0, null, true));
        }
        return elements;
    }


}
