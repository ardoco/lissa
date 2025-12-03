/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import static dev.langchain4j.internal.Utils.quoted;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.kastel.sdq.lissa.ratlr.cache.Cache;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.cache.ClassifierCacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * A classifier that uses a language model to reason about trace links between elements.
 * This classifier employs a chat-based approach to determine if elements are related,
 * using configurable prompts and caching to improve performance.
 */
public class ReasoningClassifier extends Classifier {
    /**
     * The identifier used for this classifier type in configuration and caching.
     */
    public static final String REASONING_CLASSIFIER_NAME = "reasoning";

    /**
     * The configuration key for the classification prompt.
     */
    private static final String CLASSIFICATION_PROMPT_KEY = "prompt";

    private final Cache cache;

    /**
     * Provider for the language model used in classification.
     */
    private final ChatLanguageModelProvider provider;

    /**
     * The language model instance used for classification.
     */
    private final ChatModel llm;

    /**
     * The prompt template used for classification requests.
     */
    private String prompt;

    /**
     * Whether to use original artifacts instead of nested elements.
     */
    private final boolean useOriginalArtifacts;

    /**
     * Whether to include a system message in the chat.
     */
    private final boolean useSystemMessage;

    /**
     * Creates a new reasoning classifier with the specified configuration.
     *
     * @param configuration The module configuration containing classifier settings
     * @param contextStore The shared context store for pipeline components
     */
    public ReasoningClassifier(ModuleConfiguration configuration, ContextStore contextStore) {
        super(ChatLanguageModelProvider.threads(configuration), contextStore);
        this.provider = new ChatLanguageModelProvider(configuration);
        this.cache = CacheManager.getDefaultInstance().getCache(this, provider.getCacheParameters());
        this.prompt = configuration.argumentAsStringByEnumIndex(
                CLASSIFICATION_PROMPT_KEY,
                0,
                ReasoningClassifierPrompt.values(),
                ReasoningClassifierPrompt::getPromptTemplate);
        this.useOriginalArtifacts = configuration.argumentAsBoolean("use_original_artifacts", false);
        this.useSystemMessage = configuration.argumentAsBoolean("use_system_message", true);
        this.llm = this.provider.createChatModel();
    }

    /**
     * Creates a new reasoning classifier with the specified parameters.
     * This constructor is used internally for creating thread-local copies.
     *
     * @param threads The number of threads to use for parallel processing
     * @param cache The cache to use for storing classification results
     * @param provider The language model provider
     * @param prompt The prompt template to use
     * @param useOriginalArtifacts Whether to use original artifacts
     * @param useSystemMessage Whether to include a system message
     */
    private ReasoningClassifier(
            int threads,
            Cache cache,
            ChatLanguageModelProvider provider,
            String prompt,
            boolean useOriginalArtifacts,
            boolean useSystemMessage,
            ContextStore contextStore) {
        super(threads, contextStore);
        this.cache = cache;
        this.provider = provider;
        this.prompt = prompt;
        this.useOriginalArtifacts = useOriginalArtifacts;
        this.useSystemMessage = useSystemMessage;
        this.llm = this.provider.createChatModel();
    }

    @Override
    public final Classifier copyOf() {
        return new ReasoningClassifier(
                threads, cache, provider, prompt, useOriginalArtifacts, useSystemMessage, contextStore);
    }

    @Override
    public void setClassificationPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String getClassificationPromptKey() {
        return CLASSIFICATION_PROMPT_KEY;
    }

    @Override
    public SortedMap<String, String> getCacheParameters() {
        SortedMap<String, String> providerParams = provider.getCacheParameters();
        TreeMap<String, String> classifierParams = new TreeMap<>(providerParams);
        classifierParams.put("classifier_type", REASONING_CLASSIFIER_NAME);
        return classifierParams;
    }

    /**
     * Classifies a pair of elements by using the language model to reason about their relationship.
     * The classification result is cached to avoid redundant LLM calls.
     *
     * @param source The source element
     * @param target The target element
     * @return A classification result if the elements are related, empty otherwise
     */
    @Override
    protected final Optional<ClassificationResult> classify(Element source, Element target) {
        var targetToConsider = target;
        if (useOriginalArtifacts) {
            while (targetToConsider.getParent() != null) {
                targetToConsider = targetToConsider.getParent();
            }
        }

        var sourceToConsider = source;
        /* TODO Maybe reactivate the sourceToConsider in the future ..
        if(useOriginalArtifacts){
            while (sourceToConsider.getParent() != null) {
                sourceToConsider = sourceToConsider.getParent();
            }
        }
        */

        String llmResponse = classifyIntern(sourceToConsider, targetToConsider);
        boolean isRelated = isRelated(llmResponse);
        if (isRelated) {
            return Optional.of(ClassificationResult.of(source, targetToConsider));
        }
        return Optional.empty();
    }

    /**
     * Determines if the language model's response indicates a trace link.
     * The response is expected to contain a trace tag with "yes" or "no".
     *
     * @param llmResponse The response from the language model
     * @return true if the response indicates a trace link, false otherwise
     */
    private boolean isRelated(String llmResponse) {
        Pattern pattern = Pattern.compile("<trace>(.*?)</trace>", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(llmResponse);
        boolean related = false;
        if (matcher.find()) {
            related = matcher.group().toLowerCase().contains("yes");
        } else {
            logger.debug("No trace tag found in response: {}", llmResponse);
        }
        return related;
    }

    /**
     * Performs the actual classification using the language model.
     * The result is cached to avoid redundant LLM calls.
     *
     * @param source The source element
     * @param target The target element
     * @return The language model's response
     */
    private String classifyIntern(Element source, Element target) {
        List<ChatMessage> messages = new ArrayList<>();
        if (useSystemMessage)
            messages.add(new SystemMessage(
                    "Your job is to determine if there is a traceability link between two artifacts of a system."));

        String request = prompt.replace("{source_type}", source.getType())
                .replace("{source_content}", source.getContent())
                .replace("{target_type}", target.getType())
                .replace("{target_content}", target.getContent());
        messages.add(new UserMessage(request));

        String messageString = getRepresentation(messages);
        ClassifierCacheKey cacheKey =
                ClassifierCacheKey.of(provider.modelName(), provider.seed(), provider.temperature(), messageString);

        String cachedResponse = cache.get(cacheKey, String.class);
        if (cachedResponse != null) {
            return cachedResponse;
        } else {
            logger.info(
                    "Classifying ({}): {} and {}",
                    provider.modelName(),
                    source.getIdentifier(),
                    target.getIdentifier());
            ChatResponse response = llm.chat(messages);
            String responseText = response.aiMessage().text();
            cache.put(cacheKey, responseText);
            return responseText;
        }
    }

    private String getRepresentation(List<ChatMessage> messages) {
        List<String> messageStrings =
                messages.stream().map(this::getRepresentation).toList();
        return messageStrings.toString();
    }

    private String getRepresentation(ChatMessage message) {
        return switch (message) {
            case SystemMessage systemMessage -> "SystemMessage { text = %s }".formatted(quoted(systemMessage.text()));
            case UserMessage userMessage -> {
                List<String> content = userMessage.contents().stream()
                        .map(this::getRepresentation)
                        .toList();
                yield "UserMessage { name = %s contents = %s }".formatted(quoted(userMessage.name()), content);
            }

            default ->
                throw new IllegalStateException("Unexpected message type: %s. Expected SystemMessage or UserMessage"
                        .formatted(message.getClass().getName()));
        };
    }

    private String getRepresentation(Content content) {
        return switch (content) {
            case TextContent textContent -> "TextContent { text = %s }".formatted(quoted(textContent.text()));
            default ->
                throw new IllegalStateException("Unexpected content type: %s. Expected TextContent"
                        .formatted(content.getClass().getName()));
        };
    }
}
