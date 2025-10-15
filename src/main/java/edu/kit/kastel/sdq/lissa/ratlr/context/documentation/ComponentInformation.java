package edu.kit.kastel.sdq.lissa.ratlr.context.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import edu.kit.kastel.sdq.lissa.ratlr.context.Context;
import edu.kit.kastel.sdq.lissa.ratlr.utils.tools.ProgrammaticToolProvider;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ComponentInformation implements ProgrammaticToolProvider, Context {
    
    public static final String IDENTIFIER = "documentation_component_information";
    private final Map<String, Information> components = new TreeMap<>();
    
    public ComponentInformation(Collection<Information> components) {
        components.forEach(component -> this.components.put(component.componentName, component));
    }

    public Map<String, Information> getComponents() {
        return components;
    }

    @Override
    public String getId() {
        return IDENTIFIER;
    }

    @Override
    public List<Method> getToolMethods() throws NoSuchMethodException {
        return List.of(getClass().getMethod("getComponentInformation", String.class));
    }
    
    @Tool("Returns information about packages, directories, file types and more about a component")
    public Information getComponentInformation(@P("The name of the component to retrieve information for") String componentName) {
        return components.get(componentName);
    }

    public static final class Information {
        @JsonProperty
        @Description("The name of the component")
        private String componentName;
        @JsonProperty
        @Description("Information about the components packages")
        private String packages;
        @JsonProperty
        @Description("Information about the directories that correspond to the component")
        private String directories;
        @JsonProperty
        @Description("Information about the types of files that are contained in the component")
        private String fileTypes;
        @JsonProperty
        @Description("Information about named entities that are contained in the component with instructions how to retrieve them")
        private String namedEntities;
        @JsonProperty
        @Description("Information about the purpose of the component in the project")
        private String purpose;
        @JsonProperty
        @Description("Information about whether the component is used for production or tests")
        private String production;
    }
}
