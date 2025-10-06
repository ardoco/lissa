/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.time.Duration;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * An embedding creator that uses Open WebUI for generating embeddings.
 *
 * Required environment variables:
 * <ul>
 *     <li>{@code OPENWEBUI_URL}: The URL of the Open WebUI server</li>
 *     <li>{@code OPENWEBUI_API_KEY}: API key for authentication</li>
 * </ul>
 *
 * The default model used is "nomic-embed-text:v1.5", but this can be overridden
 * through the configuration.
 */
public class OpenWebUiEmbeddingCreator extends CachedEmbeddingCreator {

    /**
     * Creates a new Open WebUI embedding creator with the specified configuration.
     * The configuration can specify a custom model name, otherwise the default
     * "nomic-embed-text:v1.5" is used.
     *
     * @param configuration The configuration containing model settings
     * @param contextStore The shared context store for pipeline components
     */
    public OpenWebUiEmbeddingCreator(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore, configuration.argumentAsString("model", "nomic-embed-text:v1.5"), 1);
    }

    /**
     * Creates an Open WebUI embedding model instance with the specified parameters.
     * The method configures the model with authentication if credentials are provided
     * in the environment variables.
     *
     * @param model The name of the Open WebUI model to use
     * @param params Additional parameters (not used in this implementation)
     * @return A configured Open WebUI embedding model instance
     */
    @Override
    protected EmbeddingModel createEmbeddingModel(String model, String... params) {
        String url = Environment.getenvNonNull("OPENWEBUI_URL");
        String apiKey = Environment.getenv("OPENWEBUI_API_KEY");

        if (url == null || apiKey == null) {
            throw new IllegalStateException("OPENWEBUI_URL or OPENWEBUI_API_KEY environment variable not set");
        }

        var openWebUiEmbeddingModel = new OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder()
                .baseUrl(url)
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofMinutes(5));
        return openWebUiEmbeddingModel.build();
    }
}
