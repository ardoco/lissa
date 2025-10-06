/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

/**
 * An embedding creator that uses Ollama for generating embeddings.
 * This class provides integration with Ollama's embedding models, supporting
 * both authenticated and unauthenticated access to the Ollama server.
 * <p>
 * Required environment variables:
 * <ul>
 *     <li>{@code OLLAMA_EMBEDDING_HOST}: The host URL of the Ollama server</li>
 *     <li>{@code OLLAMA_EMBEDDING_USER}: (Optional) Username for authentication</li>
 *     <li>{@code OLLAMA_EMBEDDING_PASSWORD}: (Optional) Password for authentication</li>
 * </ul>
 *
 * The default model used is "nomic-embed-text:v1.5", but this can be overridden
 * through the configuration.
 */
public class OllamaEmbeddingCreator extends CachedEmbeddingCreator {

    /**
     * Creates a new Ollama embedding creator with the specified configuration.
     * The configuration can specify a custom model name, otherwise the default
     * "nomic-embed-text:v1.5" is used.
     *
     * @param configuration The configuration containing model settings
     * @param contextStore The shared context store for pipeline components
     */
    public OllamaEmbeddingCreator(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore, configuration.argumentAsString("model", "nomic-embed-text:v1.5"), 1);
    }

    /**
     * Creates an Ollama embedding model instance with the specified parameters.
     * The method configures the model with authentication if credentials are provided
     * in the environment variables.
     *
     * @param model The name of the Ollama model to use
     * @param params Additional parameters (not used in this implementation)
     * @return A configured Ollama embedding model instance
     */
    @Override
    protected EmbeddingModel createEmbeddingModel(String model, String... params) {
        String host = Environment.getenvNonNull("OLLAMA_EMBEDDING_HOST");
        String user = Environment.getenv("OLLAMA_EMBEDDING_USER");
        String password = Environment.getenv("OLLAMA_EMBEDDING_PASSWORD");

        var ollamaEmbedding = new OllamaEmbeddingModel.OllamaEmbeddingModelBuilder()
                .baseUrl(host)
                .modelName(model)
                .timeout(Duration.ofMinutes(5));
        if (user != null && password != null && !user.isEmpty() && !password.isEmpty()) {
            ollamaEmbedding.customHeaders(Map.of(
                    "Authorization",
                    "Basic "
                            + Base64.getEncoder()
                                    .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8))));
        }
        return ollamaEmbedding.build();
    }
}
