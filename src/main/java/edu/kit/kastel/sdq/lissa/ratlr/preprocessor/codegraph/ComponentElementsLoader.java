package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.codegraph;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class ComponentElementsLoader extends CodeGraphPreprocessor {

    private final Preprocessor basePreprocessor;
    private final ElementRetrieval retrieval = new ElementRetrieval();
    private final ComponentFormatter formatter;
    private final CodeGraph codeGraph;
    private final boolean includeDummy;

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
        this.codeGraph = contextStore.getContext(CodeGraph.CONTEXT_IDENTIFIER, CodeGraph.class);
        this.codeGraph.initializeComponentExtraction(configuration, contextStore);
        this.includeDummy = configuration.argumentAsBoolean("include_dummy", true);
    }

    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        Map<Artifact, Element> artifactElements = getArtifactElements(artifacts);

        List<Component> components = new ArrayList<>(codeGraph.getComponents());

        List<Element> elements = new ArrayList<>(components.size());
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            Element componentInformation = new Element("component extraction" + Preprocessor.SEPARATOR + i,
                    "source code component", Jsons.writeValueAsString(new ComponentSerialization(component)), 0, null, false);
            elements.add(componentInformation);

            formatter.setValue(component);
            String content = formatter.format();
            if (content.isEmpty()) {
                continue;
            }
            Element componentElement = new Element(component.getQualifiedName(), "source code component", content, 0, componentInformation, true);
            elements.add(componentElement);

            List<Element> artifactElementsOfComponent = component.getContainedArtifacts().stream()
                    .map(artifactElements::get)
                    .toList();
            retrieval.setRetrieval(componentElement, artifactElementsOfComponent);
        }

        if (includeDummy) {
            Element componentDummy = new Element("-", "source code component dummy", "-", 0, null, true);
            ElementRetrieval elementRetrieval = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class);
            elementRetrieval.setRetrieval(componentDummy, List.of());
            elements.add(componentDummy);
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
    
    private static final class ComponentSerialization {
        @JsonProperty
        private final String simpleName;
        @JsonProperty
        private final String qualifiedName;
        @JsonIgnore
        private final SortedSet<Artifact> correspondingArtifacts;
        @JsonProperty
        private final SortedSet<String> directories;
        public ComponentSerialization(Component component) {
            this.simpleName = component.getSimpleName();
            this.qualifiedName = component.getQualifiedName();
            this.correspondingArtifacts = component.getContainedArtifacts();
            this.directories = component.getPaths();
        }
    }
}
