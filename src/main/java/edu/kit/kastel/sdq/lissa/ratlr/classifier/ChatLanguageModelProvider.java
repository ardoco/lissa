/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheParameter;
import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheParameter;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Provides chat language model instances for different platforms.
 * This class supports multiple language model platforms (OpenAI, Ollama, Blablador)
 * and handles their configuration, including authentication and model settings.
 * <p>
 * Required environment variables for each platform:
 * <ul>
 *   <li>OpenAI:
 *     <ul>
 *       <li>{@code OPENAI_ORGANIZATION_ID}: Your OpenAI organization ID</li>
 *       <li>{@code OPENAI_API_KEY}: Your OpenAI API key</li>
 *     </ul>
 *   </li>
 *   <li>Ollama:
 *     <ul>
 *       <li>{@code OLLAMA_HOST}: The host URL for the Ollama server</li>
 *       <li>{@code OLLAMA_USER}: Username for Ollama authentication (optional)</li>
 *       <li>{@code OLLAMA_PASSWORD}: Password for Ollama authentication (optional)</li>
 *     </ul>
 *   </li>
 *   <li>Blablador:
 *     <ul>
 *       <li>{@code BLABLADOR_API_KEY}: Your Blablador API key</li>
 *     </ul>
 *   </li>
 *   <li>DeepSeek:
 *   <ul>
 *       <li>{@code DEEPSEEK_API_KEY}: Your DeepSeek API key</li>
 *     </ul>
 *   </li>
 *   <li>Open WebUI:
 *   <ul>
 *       <li>{@code OPENWEBUI_URL}: The URL for the Open WebUI server</li>
 *       <li>{@code OPENWEBUI_API_KEY}: Your Open WebUI API key</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @see ChatLanguageModelPlatform
 */
public class ChatLanguageModelProvider {
    /**
     * Default seed value for model.
     */
    public static final int DEFAULT_SEED = 133742243;

    /**
     * Default temperature setting for the model.
     */
    public static final double DEFAULT_TEMPERATURE = 0.0;

    /**
     * Singleton instance cache for OpenAI chat models.
     * Key format: "modelName_seed_temperature"
     */
    private static final Map<String, OpenAiChatModel> openAiModelCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The platform to use for the language model.
     */
    private final ChatLanguageModelPlatform platform;

    /**
     * The name of the model to use.
     */
    private String modelName;

    /**
     * The seed value for model randomization.
     */
    private int seed;

    /**
     * Temperature setting for the model.
     */
    private double temperature;

    /**
     * Creates a new chat language model provider with the specified configuration.
     * The configuration name should be in the format "mode_platform" (e.g., "simple_openai").
     *
     * @param configuration The module configuration containing model settings
     */
    public ChatLanguageModelProvider(ModuleConfiguration configuration) {
        this.platform = ChatLanguageModelPlatform.fromModuleConfiguration(configuration);
        this.initPlatformParameters(configuration);
    }

    /**
     * Creates a chat model instance based on the configured platform.
     *
     * @return A chat model instance for the configured platform
     * @throws IllegalArgumentException If the platform is not supported
     */
    public ChatModel createChatModel() {
        return switch (platform) {
            case OPENAI -> createOpenAiChatModel(modelName, seed, temperature);
            case OLLAMA -> createOllamaChatModel(modelName, seed, temperature);
            case BLABLADOR -> createBlabladorChatModel(modelName, seed, temperature);
            case DEEPSEEK -> createDeepSeekChatModel(modelName, seed, temperature);
            case OPENWEBUI -> createOpenWebUIChatModel(modelName, seed, temperature);
        };
    }

    /**
     * Initializes the model platform settings from the configuration.
     * Sets the model name and seed value based on the platform.
     *
     * @param configuration The module configuration containing model settings
     * @throws IllegalArgumentException If the platform is not supported
     */
    private void initPlatformParameters(ModuleConfiguration configuration) {
        final String modelKey = "model";
        this.modelName = configuration.argumentAsString(modelKey, platform.getDefaultModel());
        this.seed = configuration.argumentAsInt("seed", DEFAULT_SEED);
        this.temperature = configuration.argumentAsDouble("temperature", DEFAULT_TEMPERATURE);
    }

    /**
     * Gets the name of the configured model.
     *
     * @return The model name
     */
    public String modelName() {
        return modelName;
    }

    /**
     * Gets the seed value used for model randomization.
     *
     * @return The seed value
     */
    public int seed() {
        return seed;
    }

    /**
     * Gets the temperature setting for the model.
     *
     * @return The temperature value
     */
    public double temperature() {
        return temperature;
    }

    /**
     * Determines the number of threads to use based on the platform.
     * OpenAI and Blablador platforms use 100 threads, while others use 1.
     *
     * @param configuration The module configuration
     * @return The number of threads to use
     */
    public static int threads(ModuleConfiguration configuration) {
        return ChatLanguageModelPlatform.fromModuleConfiguration(configuration).getThreads();
    }

    /**
     * Creates an Ollama chat model instance.
     * The model is configured with authentication if credentials are provided.
     *
     * @param model The name of the model to use
     * @param seed The seed value for randomization
     * @param temperature The temperature setting for the model
     * @return A configured Ollama chat model instance
     */
    private static OllamaChatModel createOllamaChatModel(String model, int seed, double temperature) {
        String host = Environment.getenv("OLLAMA_HOST");
        String user = Environment.getenv("OLLAMA_USER");
        String password = Environment.getenv("OLLAMA_PASSWORD");

        if (host == null) {
            throw new IllegalStateException("OLLAMA_HOST environment variable not set");
        }

        var ollama = OllamaChatModel.builder()
                .baseUrl(host)
                .modelName(model)
                .timeout(Duration.ofMinutes(10))
                .temperature(temperature)
                .seed(seed);
        if (user != null && password != null && !user.isEmpty() && !password.isEmpty()) {
            ollama.customHeaders(Map.of(
                    "Authorization",
                    "Basic "
                            + Base64.getEncoder()
                                    .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8))));
        }
        return ollama.build();
    }

    /**
     * Creates an OpenAI chat model instance.
     * Requires OpenAI organization ID and API key to be set in environment variables.
     *
     * @param model The name of the model to use
     * @param seed The seed value for randomization
     * @param temperature The temperature setting for the model
     * @return A configured OpenAI chat model instance
     * @throws IllegalStateException If required environment variables are not set
     */
    private static OpenAiChatModel createOpenAiChatModel(String model, int seed, double temperature) {
        String openAiOrganizationId = Environment.getenv("OPENAI_ORGANIZATION_ID");
        String openAiApiKey = Environment.getenv("OPENAI_API_KEY");
        if (openAiOrganizationId == null || openAiApiKey == null) {
            throw new IllegalStateException("OPENAI_ORGANIZATION_ID or OPENAI_API_KEY environment variable not set");
        }
        String cacheKey = model + "_" + seed + "_" + temperature;
        return openAiModelCache.computeIfAbsent(cacheKey, k -> new OpenAiChatModel.OpenAiChatModelBuilder()
                .modelName(model)
                .organizationId(openAiOrganizationId)
                .apiKey(openAiApiKey)
                .temperature(temperature)
                .seed(seed)
                .build());
    }

    /**
     * Creates a Blablador chat model instance.
     * Requires Blablador API key to be set in environment variables.
     *
     * @param model The name of the model to use
     * @param seed The seed value for randomization
     * @param temperature The temperature setting for the model
     * @return A configured Blablador chat model instance
     * @throws IllegalStateException If required environment variables are not set
     */
    private static OpenAiChatModel createBlabladorChatModel(String model, int seed, double temperature) {
        String blabladorApiKey = Environment.getenv("BLABLADOR_API_KEY");
        if (blabladorApiKey == null) {
            throw new IllegalStateException("BLABLADOR_API_KEY environment variable not set");
        }
        return new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.helmholtz-blablador.fz-juelich.de/v1")
                .modelName(model)
                .apiKey(blabladorApiKey)
                .temperature(temperature)
                .seed(seed)
                .build();
    }

    /**
     * Creates a DeepSeek chat model instance.
     * Requires DeepSeek API key to be set in environment variables.
     *
     * @param model The name of the model to use
     * @param seed The seed value for randomization
     * @param temperature The temperature setting for the model
     * @return A configured DeepSeek chat model instance
     * @throws IllegalStateException If required environment variables are not set
     */
    private static OpenAiChatModel createDeepSeekChatModel(String model, int seed, double temperature) {
        String deepseekApiKey = Environment.getenv("DEEPSEEK_API_KEY");
        if (deepseekApiKey == null) {
            throw new IllegalStateException("DEEPSEEK_API_KEY environment variable not set");
        }
        return new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl("https://api.deepseek.com/v1")
                .modelName(model)
                .apiKey(deepseekApiKey)
                .temperature(temperature)
                .seed(seed)
                .build();
    }

    /**
     * Creates an Open WebUI chat model instance.
     * Requires Open WebUI API key and url to be set in environment variables.
     *
     * @param model The name of the model to use
     * @param seed The seed value for randomization
     * @param temperature The temperature setting for the model
     * @return A configured Open WebUI chat model instance
     * @throws IllegalStateException If required environment variables are not set
     */
    private static ChatModel createOpenWebUIChatModel(String model, int seed, double temperature) {
        String openwebuiUrl = Environment.getenv("OPENWEBUI_URL");
        String openwebuiApiKey = Environment.getenv("OPENWEBUI_API_KEY");
        if (openwebuiUrl == null || openwebuiApiKey == null) {
            throw new IllegalStateException("OPENWEBUI_URL or OPENWEBUI_API_KEY environment variable not set");
        }

        return new OpenAiChatModel.OpenAiChatModelBuilder()
                .baseUrl(openwebuiUrl)
                .modelName(model)
                .apiKey(openwebuiApiKey)
                .temperature(temperature)
                .seed(seed)
                .timeout(Duration.ofMinutes(10))
                .build();
    }

    /**
     * Returns the parameters used to create the cache key for this model.
     * This method is used to identify the cache uniquely.
     *
     * @return An array of strings representing the cache parameters
     * @see edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager#getCache(Object, CacheParameter)
     */
    public ClassifierCacheParameter cacheParameters() {
        return new ClassifierCacheParameter(modelName, seed, temperature);
    }
}
