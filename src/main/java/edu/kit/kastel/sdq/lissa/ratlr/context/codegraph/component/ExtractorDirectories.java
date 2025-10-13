package edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CachedChatModel;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ProjectHierarchyTool;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.CodeObjectsTool;
import edu.kit.kastel.sdq.lissa.ratlr.utils.tools.Tools;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ExtractorDirectories extends ComponentExtractor {
    
    private ChatModel llmInstance;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public ExtractorDirectories(ModuleConfiguration configuration, ContextStore contextStore, Path codeRoot) {
        super(contextStore, codeRoot);
        ChatLanguageModelProvider provider = new ChatLanguageModelProvider(configuration);
        this.llmInstance = new CachedChatModel(provider, this, provider.getBuilder().strictJsonSchema(true)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA).build());
    }

    @Override
    public SortedSet<SimpleComponent> extract() {
        ProjectHierarchyTool projectTool = new ProjectHierarchyTool(codeRoot);
        CodeObjectsTool codeObjectsTool = contextStore.getContext("code_object_information_map", CodeObjectsTool.class);
        ComponentLoader loader = AiServices.builder(ComponentLoader.class)
                .chatModel(llmInstance)
                .tools(Tools.getTools(projectTool))
                .tools(Tools.getTools(codeObjectsTool))
                .build();
        
        Result<List<ComponentDirectoriesExtraction>> serviceResult = loader.load(createUserMessage());
        
        return constructComponents(serviceResult.content(), projectTool);
    }

    private static TreeSet<SimpleComponent> constructComponents(List<ComponentDirectoriesExtraction> components, ProjectHierarchyTool projectTool) {
        Map<ComponentDirectoriesExtraction, SortedSet<String>> pathsByComponent = new TreeMap<>();
        for (ComponentDirectoriesExtraction component : components) {
            
            pathsByComponent.put(component, component.productionDirectories.stream()
                    .filter(projectTool::validateDirectory)
                    .map(ExtractorDirectories::sanitizePaths)
                    .collect(Collectors.toCollection(TreeSet::new)));
            pathsByComponent.get(component).addAll(component.testDirectories.stream()
                    .filter(projectTool::validateDirectory)
                    .map(ExtractorDirectories::sanitizePaths)
                    .toList());
        }
        return pathsByComponent.keySet().stream()
                .map(component -> new PathBasedComponent(component.simpleName, component.simpleName
                        , pathsByComponent.get(component)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private String createUserMessage() {
        String codeObjectNames = contextStore.getContext("code_object_names", StringContext.class).asString();
        String componentNames = contextStore.getContext("component_names_listing", StringContext.class).asString();
        return """
                Information is retrievable for these keys:{codeObjectNames}
                
                
                Resolve the directories for these serviceResult:{componentNames}
                """
                .replace("{codeObjectNames}", codeObjectNames.startsWith("\n") ? codeObjectNames : "\n" + codeObjectNames)
                .replace("{componentNames}", componentNames.startsWith("\n") ? componentNames : "\n" + componentNames);
    }

    private static String sanitizePaths(String path) {
        return path.replaceFirst("^\\.?/?", "");
    }

    private interface ComponentLoader {
        @SystemMessage("""
                Your task is to identify software components in a project.
                First, follow the instructions to identify the production directories, i.e., those directories that contain the main purpose of the components. Multiple components usually do not share any production directories.
                Second, follow the instructions to identify the test directories. Test directories only contain elements that are related to testing the component itself.
                
                Instructions to identify directories of a component:
                1. Retrieve more information about the components you need to resolve using function `retrieveInformationForCodeObject`. Ensure to use the keys being provided.
                2. Get an overview about the projects structure using function `showSubDirectoriesOfRoot`.
                3. For each component: Make a first prediction about the directories of the components using the retrieved information about them and the project overview.
                4. If there is information about its contained packages, then check the sub-directories of its predicted directories using the function `showSubDirectoryOfDirectory`. If there is information about the components contained file types, then check the contained types of the predicted directories using the function `fileExtensionsSummary`.
                5. Adjust your directory predictions as necessary. If the predicted directory does not contain the specified packages or file types, then it is not a valid prediction for that component. A prediction is valid on the other hand, if all statements about its contained packages/sub-directories and file types hold true. The name of a directory of a component (i.e., the directory without its parents) are usually inspired by and similar to the components simple name.
                6. After that, if there are no valid predictions for a component anymore, then use the function `fileExtensionDirectories` from the root (i.e., directory ".") with the expected file type extension for that component. Start over with the 3rd step using the new information instead.
                """)
        Result<List<ComponentDirectoriesExtraction>> load(String message);
    }

    private static final class ComponentDirectoriesExtraction implements Comparable<ComponentDirectoriesExtraction> {
        @JsonProperty
        @Description("The name of the component.")
        private String simpleName;
        @JsonProperty
        @Description("The directories in the project that contain production code that corresponds to this component.")
        private List<String> productionDirectories;
        @JsonProperty
        @Description("The directories in the project that contain test cases corresponding to this component.")
        private List<String> testDirectories;
        @Override
        public int compareTo(ComponentDirectoriesExtraction o) {
            return simpleName.compareTo(o.simpleName);
        }
        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass() && compareTo((ComponentDirectoriesExtraction) o) == 0;
        }
        @Override
        public int hashCode() {
            return simpleName.hashCode();
        }
    }
    
}
