/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import static edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration.CONFIG_NAME_SEPARATOR;

import edu.kit.kastel.mcse.ardoco.llm.cache.chat.ChatCacheParameter;
import edu.kit.kastel.mcse.ardoco.llm.chat.ChatModelPlatform;
import edu.kit.kastel.mcse.ardoco.llm.chat.ChatModelProvider;
import edu.kit.kastel.mcse.ardoco.llm.chat.LlmConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Adapts LiSSA's {@link ModuleConfiguration} (whose name has the form {@code <mode>_<platform>}) to the
 * framework-neutral {@link ChatModelProvider} of the {@code llm-access} library. This keeps the existing
 * classifier call sites unchanged while delegating the actual model creation and cache identification to
 * the library.
 */
public class ChatLanguageModelProvider {

    private final ChatModelProvider delegate;

    /**
     * Creates a provider for the given module configuration.
     *
     * @param configuration The module configuration ({@code <mode>_<platform>} with optional model/seed/temperature args)
     */
    public ChatLanguageModelProvider(ModuleConfiguration configuration) {
        this.delegate = new ChatModelProvider(toLlmConfiguration(configuration));
    }

    /**
     * Creates a chat model instance for the configured platform.
     *
     * @return A chat model instance
     */
    public ChatModel createChatModel() {
        return delegate.createChatModel();
    }

    /**
     * Gets the configured model name.
     *
     * @return The model name
     */
    public String modelName() {
        return delegate.modelName();
    }

    /**
     * Gets the configured seed value.
     *
     * @return The seed value
     */
    public int seed() {
        return delegate.seed();
    }

    /**
     * Gets the configured temperature.
     *
     * @return The temperature value
     */
    public double temperature() {
        return delegate.temperature();
    }

    /**
     * Returns the cache parameters that uniquely identify the model configuration.
     *
     * @return The chat cache parameters
     */
    public ChatCacheParameter cacheParameters() {
        return delegate.cacheParameters();
    }

    /**
     * Determines the number of threads to use for the platform of the given configuration.
     *
     * @param configuration The module configuration
     * @return The number of threads to use
     */
    public static int threads(ModuleConfiguration configuration) {
        return switch (platform(configuration)) {
            case OPENAI, BLABLADOR, DEEPSEEK -> 100;
            case OPENWEBUI -> 10;
            case OLLAMA -> 1;
        };
    }

    private static LlmConfiguration toLlmConfiguration(ModuleConfiguration configuration) {
        ChatModelPlatform platform = platform(configuration);
        String model = configuration.argumentAsString("model");
        int seed = configuration.argumentAsInt("seed", LlmConfiguration.DEFAULT_SEED);
        double temperature = configuration.argumentAsDouble("temperature", LlmConfiguration.DEFAULT_TEMPERATURE);
        return LlmConfiguration.builder(platform)
                .modelName(model)
                .seed(seed)
                .temperature(temperature)
                .build();
    }

    private static ChatModelPlatform platform(ModuleConfiguration configuration) {
        String[] modeXplatform = configuration.name().split(CONFIG_NAME_SEPARATOR, 2);
        if (modeXplatform.length < 2) {
            throw new IllegalArgumentException("Invalid configuration name: '%s'. Expected format: <mode>%s<platform>"
                    .formatted(configuration.name(), CONFIG_NAME_SEPARATOR));
        }
        return ChatModelPlatform.fromString(modeXplatform[1]);
    }
}
