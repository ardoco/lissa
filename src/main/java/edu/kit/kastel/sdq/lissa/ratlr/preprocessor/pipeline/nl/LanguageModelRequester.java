package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import edu.kit.kastel.sdq.lissa.ratlr.cache.Cache;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.PipelineStage;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Futures;
import edu.kit.kastel.sdq.lissa.ratlr.utils.formatter.ContextReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.utils.formatter.TemplateFormatter;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Abstract preprocessor that enables content generation using a language model.
 * This preprocessor is part of the "summarize" type in the preprocessor hierarchy.
 * It:
 * <ul>
 *     <li>Uses a configurable template to format the system message</li>
 *     <li>Supports parallel processing with multiple threads</li>
 *     <li>Caches responses to avoid redundant querying</li>
 * </ul>
 *
 * Configuration options:
 * <ul>
 *     <li>system_message: The system message to be provided to the model</li>
 *     <li>model: The language model to use for querying</li>
 *     <li>seed: Random seed for reproducible results</li>
 * </ul>
 *
 */
public abstract class LanguageModelRequester extends PipelineStage {
    
    /** The provider for chat language models */
    private final ChatLanguageModelProvider provider;
    /** Number of threads to use for parallel processing */
    private final int threads;
    /** Cache for storing and retrieving summaries */
    private final Cache cache;
    private final TemplateFormatter defaultSystemMessageFormatter;
    private final TemplateFormatter defaultJsonSchemaFormatter;
    private final ChatModel llmInstance;
    private final Map<Integer, String> systemMessageFormatterByRequest = new HashMap<>();
    private final Map<Integer, String> jsonSchemaFormatterByRequest = new HashMap<>();

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore The shared context store for pipeline components
     */
    protected LanguageModelRequester(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
        this(moduleConfiguration, contextStore, "", "");
    }

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore The shared context store for pipeline components
     */
    protected LanguageModelRequester(ModuleConfiguration moduleConfiguration, ContextStore contextStore, 
                                     String systemMessageTemplateDefault, String jsonSchemaTemplateDefault) {
        super(contextStore);
        this.provider = new ChatLanguageModelProvider(moduleConfiguration);
        this.threads = ChatLanguageModelProvider.threads(moduleConfiguration);
        this.cache = CacheManager.getDefaultInstance().getCache(this, provider.getCacheParameters());
        
        String systemMessageTemplate = moduleConfiguration.argumentAsString("system_message", systemMessageTemplateDefault);
        if (!systemMessageTemplate.isEmpty()) {
            this.defaultSystemMessageFormatter = getTemplateFormatter(moduleConfiguration, systemMessageTemplate);
        } else {
            this.defaultSystemMessageFormatter = null;
        }
        
        String jsonSchemaTemplate = moduleConfiguration.argumentAsString("json_schema", jsonSchemaTemplateDefault);
        if (!jsonSchemaTemplate.isEmpty()) {
            this.defaultJsonSchemaFormatter = getTemplateFormatter(moduleConfiguration, jsonSchemaTemplate);
        } else {
            this.defaultJsonSchemaFormatter = null;
        }

        this.llmInstance = provider.createChatModel();
    }

    private TemplateFormatter getTemplateFormatter(ModuleConfiguration moduleConfiguration, String template) {
        return new TemplateFormatter(moduleConfiguration, new ContextReplacementRetriever(null, contextStore), template);
    }

    protected final void registerRequestSystemMessage(int requestId, String systemMessageTemplate) {
        systemMessageFormatterByRequest.put(requestId, systemMessageTemplate);
    }

    protected final void registerRequestJsonSchema(int requestId, String jsonSchemaTemplate) {
        jsonSchemaFormatterByRequest.put(requestId, jsonSchemaTemplate);
    }

    /**
     * Preprocesses a list of elements by generating content with the configured language model.
     * 
     * The requests sent to the model are created by {@link #createRequests(List)} for each given artifact. 
     * 
     * At the end {@link #createElements(List, List)} is invoked with the respective artifact and the models response.
     * 
     * This method:
     * <ol>
     *     <li>Formats summary requests using the template</li>
     *     <li>Creates a thread pool for parallel processing</li>
     *     <li>Processes requests in parallel using the language model</li>
     *     <li>Caches and retrieves summaries as needed</li>
     *     <li>Creates elements with the generated summaries</li>
     * </ol>
     *
     * The method handles parallel processing efficiently:
     * <ul>
     *     <li>Uses a thread pool with the configured number of threads</li>
     *     <li>Creates a new model instance per thread when using multiple threads</li>
     *     <li>Shares a single model instance when using one thread</li>
     * </ul>
     *
     * @param elements The list of elements to get preprocessed
     * @return A list of all created elements
     * @throws IllegalStateException if preprocessing fails
     */
    @Override
    public final List<Element> process(List<Element> elements) {
        List<Element> result = new ArrayList<>();

        List<String> requests = createRequests(elements);

        ExecutorService executorService =
                threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            String request = requests.get(i);
            tasks.add(new Task(request, formatForRequest(systemMessageFormatterByRequest.get(i), defaultSystemMessageFormatter)
                    , formatForRequest(jsonSchemaFormatterByRequest.get(i), defaultJsonSchemaFormatter)));
        }

        try {
            logger.info("Preprocessing {} elements with {} threads", elements.size(), threads);
            var futureResponses = executorService.invokeAll(tasks);
            List<String> responses = new ArrayList<>(futureResponses.size());
            for (Future<String> futureResponse : futureResponses) {
                responses.add(Futures.getLogged(futureResponse, logger));
            }
            result.addAll(createElements(elements, responses));
        } catch (InterruptedException e) {
            logger.error("Preprocessing interrupted", e);
            Thread.currentThread().interrupt();
            return result;
        } finally {
            executorService.shutdown();
        }

        cache.flush();
        return result;
    }
    
    private static String formatForRequest(String requestSpecific, TemplateFormatter formatter) {
        return requestSpecific != null ? requestSpecific : formatter != null ? formatter.format() : null;
    }
    
    private final class Task implements Callable<String> {
        
        private final String request;
        private final String systemMessage;
        private final String schema;

        private Task(String request, String systemMessage, String jsonSchema) {
            this.request = request;
            this.systemMessage = systemMessage;
            this.schema = jsonSchema;
        }

        @Override
        public String call() {
            if (request == null) {
                return null;
            }
            
            JsonSchema jsonSchema = null;
            if (this.schema != null) {
                JsonNode title = Jsons.readTree(schema).get("title");
                if (title == null || title.textValue().isEmpty()) {
                    throw new IllegalArgumentException("when using 'json_schema' then 'title' of the schema must be set and not empty");
                }
                jsonSchema = JsonSchema.builder().name(title.textValue()).rootElement(JsonRawSchema.from(schema)).build();
            }
            
            List<ChatMessage> messages = new ArrayList<>();
            if (systemMessage != null) {
                messages.add(new SystemMessage(systemMessage));
            }
            messages.add(new UserMessage(request));
            CacheKey cacheKey = CacheKey.of(
                    provider.modelName(), provider.seed(), provider.temperature(), CacheKey.Mode.CHAT
                    , messages + (jsonSchema == null ? "" : jsonSchema.toString()));

            String cachedResponse = cache.get(cacheKey, String.class);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
            if (jsonSchema != null) {
                requestBuilder.responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON)
                        .jsonSchema(jsonSchema)
                        .build());
            }
            ChatModel chatModel = threads > 1 ? provider.createChatModel() : llmInstance;
            String response = chatModel.chat(requestBuilder.build()).aiMessage().text().replace("\r", "");
            cache.put(cacheKey, response);
            return response;
        }
    }

    protected abstract List<String> createRequests(List<Element> elements);

    protected abstract List<Element> createElements(List<Element> elements, List<String> responses);
    
}
