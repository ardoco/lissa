package edu.kit.kastel.sdq.lissa.ratlr.utils.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class Tools {
    
    private Tools() {
        // utility class
    }
    
    public static <T extends ProgrammaticToolProvider> Map<ToolSpecification, ToolExecutor> getTools(T toolInstance) {
        try {
            return toolInstance.getToolMethods().stream().collect(Collectors.toMap(ToolSpecifications::toolSpecificationFrom
                    , method -> createDefaultToolExecutor(toolInstance, method)
                    , (l, r) -> l // not expected to be needed as provided ToolSpecifications are expected to be unique
                    , LinkedHashMap::new));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    // adapted from: https://github.com/langchain4j/langchain4j/blob/99b12c39370fd33707a912ef77339bab703d2dcb/langchain4j/src/main/java/dev/langchain4j/service/tool/ToolService.java#L116
    private static ToolExecutor createDefaultToolExecutor(Object object, Method method) {
        return DefaultToolExecutor.builder()
                .object(object)
                .originalMethod(method)
                .methodToInvoke(method)
                .propagateToolExecutionExceptions(true)
                .build();
    }
}
