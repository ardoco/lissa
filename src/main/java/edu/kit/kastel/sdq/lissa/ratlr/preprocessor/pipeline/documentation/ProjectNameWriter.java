package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;

import java.util.ArrayList;
import java.util.List;

public class ProjectNameWriter extends LanguageModelRequester {
    
    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = 
            "You are to interpret a software architecture documentation. Identify the name of the project. Answer by returning this name and nothing else.";
    private static final String USER_MESSAGE_FORMAT = """
            Here is the full documentation containing all sentences:
            ```
            %s
            ```
            """;

    public ProjectNameWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, "");
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        return List.of(USER_MESSAGE_FORMAT.formatted(contextStore.getContext("documentation", StringContext.class).asString()));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        contextStore.createContext(new StringContext("project_name", responses.getFirst()));
        elements.forEach(element -> element.setCompare(true));
        List<Element> results = new ArrayList<>(elements);
        results.add(new Element("project name", "meta information", responses.getFirst(), 0, elements.getFirst(), false));
        return results;
    }
}
