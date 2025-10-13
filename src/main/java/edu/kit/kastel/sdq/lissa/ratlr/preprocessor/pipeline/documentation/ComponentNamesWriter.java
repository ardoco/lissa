package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;

import java.util.List;

public class ComponentNamesWriter extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            You are to interpret a software architecture documentation. Identify main components that are explicitly mentioned in the documentation.
            Ensure hereby the following:
            - They are simple names excluding the name of the project (`<<<context_project_name>>>`)
            - They exclude prefixes and suffixes that are not essential
            - They represent main components of the project
            
            ## Output Format
            Return a bulleted list of the names and nothing else.
            """;
    private static final String USER_MESSAGE_FORMAT = """
            Here is the full documentation containing all sentences:
            ```
            %s
            ```
            """;

    public ComponentNamesWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, "");
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {        
        return List.of(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        String componentNamesListing = responses.getFirst();
        contextStore.createContext(new StringContext("component_names_listing", componentNamesListing));
        String[] namesSplit = componentNamesListing.split("\n?- ");
        contextStore.createContext(new StringContext("component_names_json_list", 
                "\"" + String.join("\", \"", namesSplit) + "\""));
        contextStore.createContext(new StringContext("component_names_count", String.valueOf(namesSplit.length)));
        
        elements.forEach(element -> element.setCompare(true));
        return elements;
    }
}
