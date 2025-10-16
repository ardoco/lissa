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
import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.ProjectHierarchyTool;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentInformation;
import edu.kit.kastel.sdq.lissa.ratlr.context.documentation.ComponentNames;
import edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator.EmbeddingCreator;
import edu.kit.kastel.sdq.lissa.ratlr.utils.tools.Tools;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class ExtractorDirectories extends ComponentExtractor {

    private static final String USER_MESSAGE_TEMPLATE = """
            Information is retrievable for the keys: {componentNames}
            
            Resolve the directories for the component: `{componentName}`
            """;
    private final ChatModel llmInstance;
    private final String userMessageTemplate;
    private final EmbeddingCreator embeddingCreator;
    private int noMatchK;

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
        this.userMessageTemplate = configuration.argumentAsString("user_message_template", USER_MESSAGE_TEMPLATE);
        configuration.argumentAsString("load_production_system_message", ComponentLoader.LOAD_PRODUCTION_SYSTEM_MESSAGE);
        configuration.argumentAsString("load_test_system_message", ComponentLoader.LOAD_TEST_SYSTEM_MESSAGE);
        Map<String, String> embeddingArguments = new HashMap<>();
        embeddingArguments.put("model", configuration.argumentAsString("embedding_model", "text-embedding-3-large"));
        this.embeddingCreator = EmbeddingCreator.createEmbeddingCreator(
                new ModuleConfiguration(configuration.argumentAsString("embedding_platform"), embeddingArguments), contextStore);
        this.noMatchK = configuration.argumentAsInt("no_match_k", 5);
    }

    @Override
    public SortedSet<SimpleComponent> extract() {
        ProjectHierarchyTool projectTool = new ProjectHierarchyTool(codeRoot, 4, embeddingCreator, noMatchK);
        ComponentInformation componentInformation = contextStore.getContext(ComponentInformation.IDENTIFIER, ComponentInformation.class);
        SortedSet<SimpleComponent> components = new TreeSet<>();
        for (String componentName : contextStore.getContext("documentation_component_names", ComponentNames.class).getNames()) {
            if (componentName.isEmpty()) {
                continue;
            }
            
            ComponentLoader loader = AiServices.builder(ComponentLoader.class)
                    .chatModel(llmInstance)
                    .tools(Tools.getTools(projectTool))
                    .tools(Tools.getTools(componentInformation))
                    .build();
            Result<ComponentDirectoriesExtraction> productionResult = loader.loadProduction(createUserMessage(componentName, 
                    componentInformation.getComponents().keySet()));
            Result<ComponentDirectoriesExtraction> testResult = loader.loadTest(createUserMessage(componentName, 
                    componentInformation.getComponents().keySet()));
            components.add(constructComponent(componentName, productionResult.content(), testResult.content(), projectTool));
        }
        return components;
    }

    private static SimpleComponent constructComponent(String simpleName, ComponentDirectoriesExtraction productionDirectories,
                                                      ComponentDirectoriesExtraction testDirectories, ProjectHierarchyTool projectTool) {
        SortedSet<String> paths = new TreeSet<>();
        addDirectories(productionDirectories, projectTool, paths);
        addDirectories(testDirectories, projectTool, paths);
        return new PathBasedComponent(simpleName, simpleName, paths);
    }

    private static void addDirectories(ComponentDirectoriesExtraction extraction, ProjectHierarchyTool projectTool, SortedSet<String> paths) {
        if (extraction.directories != null) {
            extraction.directories.stream()
                    .filter(projectTool::validateDirectory)
                    .map(ExtractorDirectories::sanitizePaths)
                    .forEach(paths::add);
        }
    }

    private String createUserMessage(String componentName, Collection<String> componentNames) {
        return userMessageTemplate
                .replace("{componentNames}", "`" + String.join("`, `", componentNames) + "`")
                .replace("{componentName}", componentName);
    }

    private static String sanitizePaths(String path) {
        return path.replaceFirst("^\\.?/?", "");
    }

    private interface ComponentLoader {

        String LOAD_PRODUCTION_SYSTEM_MESSAGE = """
                Your task is to identify directories in a software project that likely correspond to a software component for which you'll be given its name.
                Follow the instructions to identify the production directories, i.e., those directories that contain the main purpose of the component.
                
                Instructions to identify directories of a component:
                1. Retrieve more information about the component you need to resolve using function `retrieveInformationForCodeObject`. Ensure to use the keys being provided.
                2. Try to find directories with the name of the component using `findDirectories`.
                3. Make a first prediction about the directories of the component using the retrieved information about them and the project overview.
                4. If there is information about its contained packages, then check the sub-directories of its predicted directories using the function `showSubDirectoryOfDirectory`. If there is information about the components contained file types, then check the contained types of the predicted directories using the function `fileExtensionsSummary`.
                5. Adjust your directory predictions as necessary. If the predicted directory does not contain the specified packages or file types, then it is not a valid prediction for that component. A prediction is valid on the other hand, if all statements about its contained packages/sub-directories and file types hold true. The name of a directory of a component (i.e., the directory without its parents) are usually inspired by and similar to the components simple name.
                6. After that, if there are no valid predictions for a component anymore, then use the function `fileExtensionDirectories` from the root (i.e., directory ".") with the expected file type extension for that component. Start over with the 3rd step using the new information instead.
                """;
        String LOAD_TEST_SYSTEM_MESSAGE = """
                Your task is to identify directories in a software project that likely correspond to a software component for which you'll be given its name.
                Follow the instructions to identify the test directories. Test directories only contain elements that are related to testing the component itself.
                
                Instructions to identify directories of a component:
                1. Retrieve more information about the component you need to resolve using function `retrieveInformationForCodeObject`. Ensure to use the keys being provided.
                2. Try to find directories with the name of the component using `findDirectories`.
                3. Make a first prediction about the directories of the component using the retrieved information about them and the project overview.
                4. If there is information about its contained packages, then check the sub-directories of its predicted directories using the function `showSubDirectoryOfDirectory`. If there is information about the components contained file types, then check the contained types of the predicted directories using the function `fileExtensionsSummary`.
                5. Adjust your directory predictions as necessary. If the predicted directory does not contain the specified packages or file types, then it is not a valid prediction for that component. A prediction is valid on the other hand, if all statements about its contained packages/sub-directories and file types hold true. The name of a directory of a component (i.e., the directory without its parents) are usually inspired by and similar to the components simple name.
                6. After that, if there are no valid predictions for a component anymore, then use the function `fileExtensionDirectories` from the root (i.e., directory ".") with the expected file type extension for that component. Start over with the 3rd step using the new information instead.
                """;

        @SystemMessage(LOAD_PRODUCTION_SYSTEM_MESSAGE)
        Result<ComponentDirectoriesExtraction> loadProduction(String message);

        @SystemMessage(LOAD_TEST_SYSTEM_MESSAGE)
        Result<ComponentDirectoriesExtraction> loadTest(String message);
    }

    private static final class ComponentDirectoriesExtraction implements Comparable<ComponentDirectoriesExtraction> {
        @JsonProperty
        @Description("The name of the component.")
        private String simpleName;
        @JsonProperty
        @Description("The directories in the project that correspond to this component.")
        private List<String> directories;
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
