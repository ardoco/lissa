package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ElementRetrieval;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleArtifactPreprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentElementsLoader extends CodeGraphPreprocessor {

    private final Preprocessor basePreprocessor;
    private final ElementRetrieval retrieval = new ElementRetrieval();
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public ComponentElementsLoader(ContextStore contextStore) {
        super(contextStore);
        this.basePreprocessor = new SingleArtifactPreprocessor(contextStore);
        contextStore.createContext(retrieval);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        Map<Artifact, Element> artifactElements = getArtifactElements(artifacts);
        
        CodeGraph codeGraph = contextStore.getContext(CodeGraphProvider.CONTEXT_IDENTIFIER, CodeGraph.class);
        Collection<Component> components = codeGraph.getComponents();

        List<Element> elements = new ArrayList<>(components.size());
        for (Component component : components) {
            Element componentElement = new Element(component.getSimpleName(), "source code component", component.getQualifiedName(), 0, null, true);
            elements.add(componentElement);

            List<Element> artifactElementsOfComponent = codeGraph.getContainedArtifacts(component).stream()
                    .map(artifactElements::get)
                    .toList();
            retrieval.setRetrieval(componentElement, artifactElementsOfComponent);
        }
        return elements;
    }

    private Map<Artifact, Element> getArtifactElements(List<Artifact> artifacts) {
        List<Element> elements = basePreprocessor.preprocess(artifacts);
        Map<Artifact, Element> map = new LinkedHashMap<>();
        for (int i = 0; i < artifacts.size(); i++) {
            map.put(artifacts.get(i), elements.get(i));
        }
        return map;
    }
}
