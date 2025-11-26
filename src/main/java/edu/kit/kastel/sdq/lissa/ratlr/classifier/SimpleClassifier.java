/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.Optional;

import edu.kit.kastel.sdq.lissa.ratlr.cache.Cache;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.KeyGenerator;

import dev.langchain4j.model.chat.ChatModel;

/**
 * A simple classifier that uses a language model to determine trace links between elements.
 * This classifier uses a straightforward yes/no approach, asking the language model
 * directly whether elements are related. It includes caching to improve performance
 * and supports custom templates for the classification request.
 */
public class SimpleClassifier extends Classifier {

    /**
     * The default template for classification requests.
     * This template presents two artifacts and asks if they are related.
     */
    private static final String DEFAULT_TEMPLATE =
            """
                    Question: Here are two parts of software development artifacts.

                    {source_type}: '''{source_content}'''

                    {target_type}: '''{target_content}'''
                    Are they related?

                    Answer with 'yes' or 'no'.
                    """;

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
     * The template used for classification requests.
     */
    private String template;

    /**
     * Creates a new simple classifier with the specified configuration.
     *
     * @param configuration The module configuration containing classifier settings
     */
    public SimpleClassifier(ModuleConfiguration configuration) {
        super(ChatLanguageModelProvider.threads(configuration));
        this.provider = new ChatLanguageModelProvider(configuration);
        this.template = configuration.argumentAsString("template", DEFAULT_TEMPLATE);
        this.cache = CacheManager.getDefaultInstance()
                .getCache(this.getClass().getSimpleName() + "_" + provider.modelName() + "_" + provider.seed());
        this.llm = provider.createChatModel();
    }

    /**
     * Creates a new simple classifier with the specified parameters.
     * This constructor is used internally for creating thread-local copies.
     *
     * @param threads  The number of threads to use for parallel processing
     * @param cache    The cache to use for storing classification results
     * @param provider The language model provider
     * @param template The template to use for classification requests
     */
    private SimpleClassifier(int threads, Cache cache, ChatLanguageModelProvider provider, String template) {
        super(threads);
        this.cache = cache;
        this.provider = provider;
        this.template = template;
        this.llm = provider.createChatModel();
    }

    /**
     * Creates a copy of this simple classifier.
     * This method is used for parallel processing.
     *
     * @return A new simple classifier instance with the same configuration
     */
    @Override
    protected final Classifier copyOf() {
        return new SimpleClassifier(threads, cache, provider, template);
    }

    /**
     * Classifies a pair of elements by using the language model to determine if they are related.
     * The classification result is cached to avoid redundant LLM calls.
     *
     * @param source The source element
     * @param target The target element
     * @return A classification result if the elements are related, empty otherwise
     */
    @Override
    protected final Optional<ClassificationResult> classify(Element source, Element target) {
        String llmResponse = classifyIntern(source, target);

        String thinkEnd = "</think>";
        if (llmResponse.startsWith("<think>") && llmResponse.contains(thinkEnd)) {
            // Omit the thinking of models like deepseek-r1
            llmResponse = llmResponse
                    .substring(llmResponse.indexOf(thinkEnd) + thinkEnd.length())
                    .strip();
        }

        boolean isRelated = llmResponse.toLowerCase().contains("yes");
        if (isRelated) {
            return Optional.of(ClassificationResult.of(source, target));
        }

        return Optional.empty();
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
        String request = template.replace("{source_type}", source.getType())
                .replace("{source_content}", source.getContent())
                .replace("{target_type}", target.getType())
                .replace("{target_content}", target.getContent());

        String key = KeyGenerator.generateKey(request);
        CacheKey cacheKey = new CacheKey(provider.modelName(), provider.seed(), CacheKey.Mode.CHAT, request, key);
        String cachedResponse = cache.get(cacheKey, String.class);
        if (cachedResponse != null) {
            return cachedResponse;
        } else {
            logger.info(
                    "Classifying ({}): {} and {}",
                    provider.modelName(),
                    source.getIdentifier(),
                    target.getIdentifier());
            String response = llm.chat(request);
            cache.put(cacheKey, response);
            return response;
        }
    }

    /**
     * Returns the current classification template used by this classifier.
     *
     * @return The prompt template as a string
     */
    @Override
    public String getPrompt() {
        return this.template;
    }

    /**
     * Sets a new classification template for this classifier.
     * This allows dynamic modification of the prompt used in trace link classification.
     *
     * @param prompt The new prompt template to use
     */
    @Override
    public void setPrompt(String prompt) {
        this.template = prompt;
    }
}
