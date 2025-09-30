package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.codegraph;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.artifactprovider.CodeGraphProvider;
import edu.kit.kastel.sdq.lissa.ratlr.context.CodeGraph;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.ElementRetrieval;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.SimpleComponent;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleArtifactPreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class ComponentCreator extends SingleElementProcessingStage {
    
    private static final String COMPONENT_SCHEMA = """
            {
                "$schema": "https://json-schema.org/draft/2020-12/schema",
                "title": "Extraction",
                "description": "The extracted component with its name and corresponding directories",
                "type": "object",
                "properties": {
                    "name": {
                        "description": "The name of the component",
                        "type": "string"
                    },
                    "directories": {
                        "description": "The directories that correspond to the component",
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "minItems": 1
                    }
                },
                "required": ["name", "directories"]
            }""";

    private final Preprocessor basePreprocessor;
    
    public ComponentCreator(ContextStore contextStore) {
        super(contextStore);
        this.basePreprocessor = new SingleArtifactPreprocessor(contextStore);
    }

    @Override
    public List<Element> process(List<Element> elements) {
        List<Element> results = new ArrayList<>(elements.size() + 1);
        Element componentDummy = new Element("-", "source code component dummy", "-", 0, null, true);
        ElementRetrieval elementRetrieval = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class);
        elementRetrieval.setRetrieval(componentDummy, List.of());
        
        results.add(componentDummy);
        results.addAll(super.process(elements));
        return results;
    }

    @Override
    protected List<Element> process(Element element) {
        String json = element.getContent();
        ComponentInformation componentInformation = Jsons.readValue(json, new TypeReference<>() {});
        CodeGraph codeGraph = contextStore.getContext(CodeGraphProvider.CONTEXT_IDENTIFIER, CodeGraph.class);
        
        Collection<Artifact> allArtifacts = codeGraph.getArtifacts();
        SortedSet<Artifact> containedArtifacts = new TreeSet<>(Comparator.comparing(Artifact::getIdentifier));
        SortedSet<String> paths = new TreeSet<>();
        for (String directory : componentInformation.directories) {
            paths.add(directory);
            for (Artifact artifact : allArtifacts) {
                if (artifact.getIdentifier().startsWith(directory)) {
                    containedArtifacts.add(artifact);
                }
            }
        }

        SimpleComponent component = new SimpleComponent(componentInformation.name, componentInformation.name, containedArtifacts, paths);
        Element result = new Element(componentInformation.name, "source code component", componentInformation.name, 0, null, true);
        
        ElementRetrieval elementRetrieval = contextStore.getContext(ElementRetrieval.IDENTIFIER, ElementRetrieval.class);
        elementRetrieval.setRetrieval(result, basePreprocessor.preprocess(new ArrayList<>(containedArtifacts)));

        return List.of(result);
    }
    
    private static final class ComponentInformation {
        
        @JsonProperty
        private String name;
        @JsonProperty
        private String[] directories;
    }
}
