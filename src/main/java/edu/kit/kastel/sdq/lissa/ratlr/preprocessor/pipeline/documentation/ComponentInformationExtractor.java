package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentInformation;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentNames;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.PipelinePreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComponentInformationExtractor extends LanguageModelRequester {

    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT = """
            Your task is to analyse software architecture documentation and collect information about components in the project.
            Each sentence of the documentation is prefixed with its identifier.
            """;
    private static final String USER_MESSAGE_FORMAT = """
            Full documentation:
            ```
            %s
            ```
            
            The component to analyze: `%s`
            """;
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"ComponentInformationExtraction\",\"description\": \"Extraction of component information in a sentence\",\"type\": \"object\",\"properties\": {\"componentName\": {\"description\": \"The name of the component to retrieve information for\",\"type\": \"string\"},\"packages\": {\"description\": \"Information about the components packages\",\"type\": \"string\"},\"directories\": {\"description\": \"Information about the directories that correspond to the component\",\"type\": \"string\"},\"fileTypes\": {\"description\": \"Information about the types of files that are contained in the component\",\"type\": \"string\"},\"namedEntities\": {\"description\": \"Information about named entities that are contained in the component with instructions how to retrieve them\",\"type\": \"string\"},\"purpose\": {\"description\": \"Information about the purpose of the component in the project\",\"type\": \"string\"},\"production\": {\"description\": \"Information about whether the component is used for production or tests\",\"type\": \"string\"}},\"required\": [\"componentName\",\"packages\", \"directories\", \"fileTypes\", \"namedEntities\", \"purpose\", \"production\"]}";

    public ComponentInformationExtractor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        List<String> requests = new ArrayList<>();
        for (String name : contextStore.getContext(ComponentNames.IDENTIFIER, ComponentNames.class).getNames()) {
            requests.add(USER_MESSAGE_FORMAT.formatted(PipelinePreprocessor.getLineIdPrefixedDocumentation(contextStore), name));
        }
        return requests;
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        List<Element> results = new ArrayList<>(responses.size());
        Collection<ComponentInformation.Information> collectedInformation = new ArrayList<>(responses.size());
        for (int i = 0; i < responses.size(); i++) {
            String response = responses.get(i);
            results.add(new Element("component information", "meta information", response, 0, elements.getFirst(), false));

            collectedInformation.add(Jsons.readValue(response, new TypeReference<>() {}));
        }
        contextStore.createContext(new ComponentInformation(collectedInformation));

        elements.forEach(element -> element.setCompare(true));
        results.addAll(elements);
        return results;
    }
}
