package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentNames;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.List;

public class ComponentNamesWriter extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            You are to interpret a software architecture documentation. Identify main components that are explicitly mentioned in the documentation.
            Ensure hereby the following:
            - They are simple names excluding the name of the project (`<<<context_project_name>>>`)
            - They exclude prefixes and suffixes that are not essential
            - They represent main components of the project
            """;
    private static final String USER_MESSAGE_FORMAT = """
            Here is the full documentation containing all sentences:
            ```
            %s
            ```
            """;
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT = 
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"ComponentNamesIdentification\",\"description\": \"Identification of explicitly mentioned main components in the documentation.\",\"type\": \"object\",\"properties\": {\"names\": {\"description\": \"Explicitly mentioned main components in the documentation.\",\"type\": \"array\",\"items\": {\"type\": \"string\"}}},\"required\": [\"names\"]}";

    public ComponentNamesWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {        
        return List.of(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        ComponentNames componentNames = Jsons.readValue(responses.getFirst(), new TypeReference<>() {});
        contextStore.createContext(componentNames);
        contextStore.createContext(new StringContext("documentation_component_names_json", componentNames.asJsonArrayWithoutBrackets()));

        elements.forEach(element -> element.setCompare(true));
        List<Element> results = new ArrayList<>(elements);
        results.add(new Element("component names", "meta information", responses.getFirst(), 0, elements.getFirst(), false));
        return results;
    }
}
