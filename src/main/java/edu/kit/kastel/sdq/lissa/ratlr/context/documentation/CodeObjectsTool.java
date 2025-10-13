package edu.kit.kastel.sdq.lissa.ratlr.context.documentation;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import edu.kit.kastel.sdq.lissa.ratlr.context.Context;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;
import edu.kit.kastel.sdq.lissa.ratlr.utils.tools.ProgrammaticToolProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class CodeObjectsTool implements ProgrammaticToolProvider, Context {

    private final String contextIdentifier;
    private final Map<String, List<String>> codeObjectInformation;

    public CodeObjectsTool(String contextIdentifier, Map<String, List<String>> codeObjectInformation) {
        this.contextIdentifier = contextIdentifier;
        this.codeObjectInformation = codeObjectInformation;
    }

    @Override
    public List<Method> getToolMethods() throws NoSuchMethodException {
        return List.of(getClass().getMethod("retrieveInformationForCodeObject", String.class));
    }

    @Tool("Retrieves information about a resolvable code object.")
    public String retrieveInformationForCodeObject(@P("The code object to retrieve information for.") String codeObject) {
        List<String> information = codeObjectInformation.get(codeObject);
        System.out.println("Accessed '%s' with result '%s'".formatted(codeObject, information));
        return information == null ? "Error, unknown code object." : Jsons.writeValueAsString(information);
    }

    @Override
    public String getId() {
        return contextIdentifier;
    }
}
