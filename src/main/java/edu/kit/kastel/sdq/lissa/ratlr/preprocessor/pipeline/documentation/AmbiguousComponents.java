package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.AmbiguityMapper;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentNames;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class AmbiguousComponents extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            You'll be given a list of components in a software project that are described in the documentation.
            Your task is to identify pairs of components that share ambiguities.
            These might arise through similar names, inconsistent use in the documentation or being part of a named structure that does not distinguish between them.
            Other reasons for ambiguities are also possible.
            
            ## Output
            Return a list of ambiguity cases and provide information about how these ambiguities can be resolved when reading a sentence of the documentation.
            """;
    private static final String USER_MESSAGE_FORMAT = """
            List of components:
            %s
            
            Full documentation:
            ```
            %s
            ```
            """;
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT = 
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"AmbiguityExtraction\",\"description\": \"Extraction of ambiguous components with information how to resolve these cases.\",\"type\": \"object\",\"properties\": {\"cases\": {\"description\": \"The list of extracted ambiguity cases.\",\"type\": \"array\",\"items\": {\"description\": \"An ambiguity case with information about how to resolve it.\",\"type\": \"object\",\"properties\": {\"ambiguousComponents\": {\"description\": \"The components that share ambiguity.\",\"type\": \"array\",\"items\": {\"type\": \"string\"}},\"additionalInformation\": {\"description\": \"Information about the ambiguity and how to resolve it to be able to distinguish the components when reading the documentation.\",\"type\": \"string\"}},\"required\": [\"ambiguousComponents\", \"additionalInformation\"]}}},\"required\": [\"cases\"]}";

    public AmbiguousComponents(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        return List.of(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation_component_names", ComponentNames.class).asListing(),
                contextStore.getContext("documentation", StringContext.class).asString()));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        contextStore.createContext(new AmbiguityMapper("component_ambiguity_mapper",
                Jsons.readValue(responses.getFirst(), new TypeReference<AmbiguityExtraction>() {}).cases.stream()
                        .collect(Collectors.toMap(AmbiguityCase::getAmbiguousComponents, AmbiguityCase::getAdditionalInformation))));
        
        elements.forEach(element -> element.setCompare(true));
        return elements;
    }
    
    private static final class AmbiguityExtraction {
        @JsonProperty
        private List<AmbiguityCase> cases;
    }
    
    private static final class AmbiguityCase {
        @JsonProperty
        private SortedSet<String> ambiguousComponents;
        @JsonProperty
        private String additionalInformation;
        
        public SortedSet<String> getAmbiguousComponents() {
            return ambiguousComponents;
        }
        public String getAdditionalInformation() {
            return additionalInformation;
        }
    }
}
