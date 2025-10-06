/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

/**
 * Enum representing supported chat language model platforms.
 * Each platform specifies the number of threads to use for parallel execution.
 *
 * <ul>
 *   <li>OPENAI: OpenAI platform (100 threads)</li>
 *   <li>OLLAMA: Ollama platform (1 thread)</li>
 *   <li>BLABLADOR: Blablador platform (100 threads)</li>
 *   <li>DEEPSEEK: DeepSeek platform (1 thread)</li>
 * </ul>
 *
 * @see ChatLanguageModelProvider
 */
public enum ChatLanguageModelPlatform {
    /**
     * OpenAI platform (100 threads).
     */
    OPENAI(100, "gpt-4o-mini"),
    /**
     * Ollama platform (1 thread).
     */
    OLLAMA(1, "llama3:8b"),
    /**
     * Blablador platform (100 threads).
     */
    BLABLADOR(100, "2 - Llama 3.3 70B instruct"),
    /**
     * DeepSeek platform (1 thread).
     */
    DEEPSEEK(1, "deepseek-chat"),
    /**
     * Open WebUI platform (1 thread, default model: "llama3:8b").
     */
    OPENWEBUI(1, "llama3:8b");

    private final int threads;
    private final String defaultModel;

    ChatLanguageModelPlatform(int threads, String defaultModel) {
        this.threads = threads;
        this.defaultModel = defaultModel;
    }

    /**
     * Returns the number of threads for this platform.
     *
     * @return the thread count
     */
    public int getThreads() {
        return threads;
    }

    /**
     * Returns the default model name for this platform.
     *
     * @return the default model name
     */
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * Returns the enum value for the given platform name (case-insensitive).
     *
     * @param moduleConfiguration the configuration containing the platform name
     * @return the corresponding enum value
     * @throws IllegalArgumentException if the name does not match any platform
     */
    public static ChatLanguageModelPlatform fromModuleConfiguration(ModuleConfiguration moduleConfiguration) {
        String[] modeXplatform = moduleConfiguration.name().split(Classifier.CONFIG_NAME_SEPARATOR, 2);
        if (modeXplatform.length < 2) {
            throw new IllegalArgumentException("Invalid configuration name: '%s'. Expected format: <mode>%s<platform>"
                    .formatted(moduleConfiguration.name(), Classifier.CONFIG_NAME_SEPARATOR));
        }

        String name = modeXplatform[1];

        for (ChatLanguageModelPlatform languageModelPlatform : values()) {
            if (languageModelPlatform.name().equalsIgnoreCase(name)) {
                return languageModelPlatform;
            }
        }
        throw new IllegalArgumentException("Unknown platform: " + name);
    }
}
