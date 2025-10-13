package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.codegraph;

import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.ElementRetrieval;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleArtifactPreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.utils.formatter.ComponentFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ComponentElementsLoader extends CodeGraphPreprocessor {

    private final Preprocessor basePreprocessor;
    private final ElementRetrieval retrieval = new ElementRetrieval();
    private final ComponentFormatter formatter;
    private final CodeGraph codeGraph;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public ComponentElementsLoader(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.basePreprocessor = new SingleArtifactPreprocessor(contextStore);
        contextStore.createContext(retrieval);
        this.formatter = new ComponentFormatter(configuration, contextStore);
        this.codeGraph = contextStore.getContext(CodeGraphProvider.CONTEXT_IDENTIFIER, CodeGraph.class);
        this.codeGraph.initializeComponentExtraction(configuration, contextStore);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        Map<Artifact, Element> artifactElements = getArtifactElements(artifacts);
        
        Collection<Component> components = codeGraph.getComponents();

        List<Element> elements = new ArrayList<>(components.size());
        for (Component component : components) {
            formatter.setValue(component);
            
            Element componentElement = new Element(component.getQualifiedName(), "source code component", formatter.format(), 0, null, true);
            elements.add(componentElement);

            List<Element> artifactElementsOfComponent = component.getContainedArtifacts().stream()
                    .map(artifactElements::get)
                    .toList();
            retrieval.setRetrieval(componentElement, artifactElementsOfComponent);
        }

        Element componentDummy = new Element("-", "source code component dummy", "-", 0, null, true);
        ElementRetrieval elementRetrieval = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class);
        elementRetrieval.setRetrieval(componentDummy, List.of());
        elements.add(componentDummy);
        
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
