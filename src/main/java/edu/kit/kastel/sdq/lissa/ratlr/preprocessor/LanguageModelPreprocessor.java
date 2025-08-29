package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

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
import edu.kit.kastel.sdq.lissa.ratlr.utils.Futures;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public abstract class LanguageModelPreprocessor extends Preprocessor<Element> {
    
    /** The provider for chat language models */
    private final ChatLanguageModelProvider provider;
    /** Number of threads to use for parallel processing */
    private final int threads;
    /** Cache for storing and retrieving summaries */
    private final Cache cache;
    private final String systemMessage;
    private final String jsonSchema;
    private final ChatModel llmInstance;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore The shared context store for pipeline components
     */
    protected LanguageModelPreprocessor(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
        super(contextStore);
        this.provider = new ChatLanguageModelProvider(moduleConfiguration);
        this.threads = ChatLanguageModelProvider.threads(moduleConfiguration);
        this.cache = CacheManager.getDefaultInstance().getCache(this, provider.getCacheParameters());
        this.systemMessage = moduleConfiguration.argumentAsString("system_message", "");
        this.jsonSchema = moduleConfiguration.argumentAsString("json_schema", "");
        this.llmInstance = provider.createChatModel();
    }

    /**
     * Preprocesses a list of elements by generating content with the configured language model.
     * 
     * The request sent to the model is created by {@link #createRequest(Element)} for each given artifact. 
     * 
     * At the end {@link #createElements(Element, String)} is invoked with the respective artifact and the models response.
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
    public final List<Element> preprocess(List<Element> elements) {
        List<Element> result = new ArrayList<>();

        List<String> requests = new ArrayList<>();

        for (Element element : elements) {
            String request = createRequest(element);
            if (request == null || request.isEmpty()) {
                logger.error("Request for element {} must not be null or empty", element.getIdentifier());
                // TODO react properly
            }
            requests.add(request);
        }

        ExecutorService executorService =
                threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();

        List<Callable<String>> tasks = new ArrayList<>();
        for (String request : requests) {
            tasks.add(new Task(request));
        }

        try {
            logger.info("Preprocessing {} elements with {} threads", elements.size(), threads);
            var responses = executorService.invokeAll(tasks);
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                String response = Futures.getLogged(responses.get(i), logger);
                result.addAll(createElements(element, response));
            }
        } catch (InterruptedException e) {
            logger.error("Preprocessing interrupted", e);
            Thread.currentThread().interrupt();
            return result;
        } finally {
            executorService.shutdown();
        }

        return result;
    }
    
    private final class Task implements Callable<String> {
        
        private final String request;

        private Task(String request) {
            this.request = request;
        }

        @Override
        public String call() {
            CacheKey cacheKey = CacheKey.of(
                    provider.modelName(), provider.seed(), provider.temperature(), CacheKey.Mode.CHAT, request);

            String cachedResponse = cache.get(cacheKey, String.class);
            if (cachedResponse != null) {
                return cachedResponse;
            }
            
            ChatModel chatModel = threads > 1 ? provider.createChatModel() : llmInstance;
            List<ChatMessage> messages = new ArrayList<>();
            if (!systemMessage.isEmpty()) {
                messages.add(new SystemMessage(systemMessage));
            }
            messages.add(new UserMessage(request));

            ChatRequest.Builder requestBuilder = ChatRequest.builder().messages(messages);
            if (!jsonSchema.isEmpty()) {
                requestBuilder.responseFormat(ResponseFormat.builder().type(ResponseFormatType.JSON)
                        .jsonSchema(JsonSchema.builder().rootElement(JsonRawSchema.from(jsonSchema)).build())
                        .build());
            }
            String response = chatModel.chat(requestBuilder.build()).aiMessage().text().replace("\r", "");
            cache.put(cacheKey, response);
            return response;
        }
    }

    protected abstract String createRequest(Element element);

    protected abstract List<Element> createElements(Element element, String response);
    
}
