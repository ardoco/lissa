/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

/**
 * An embedding creator that uses OpenAI's embedding models for generating embeddings.
 * This class provides integration with OpenAI's embedding API, supporting high-throughput
 * embedding generation through parallel processing.
 * <p>
 * Required environment variables:
 * <ul>
 *     <li>{@code OPENAI_ORGANIZATION_ID}: Your OpenAI organization ID</li>
 *     <li>{@code OPENAI_API_KEY}: Your OpenAI API key</li>
 * </ul>
 *
 * The default model used is "text-embedding-ada-002", but this can be overridden
 * through the configuration. The creator uses 40 threads by default for parallel
 * processing of embedding requests.
 */
public class OpenAiEmbeddingCreator extends CachedEmbeddingCreator {
    /** Default number of threads for parallel processing */
    private static final int THREADS = 40;

    /**
     * Creates a new OpenAI embedding creator with the specified configuration.
     * The configuration can specify a custom model name, otherwise the default
     * "text-embedding-ada-002" is used.
     *
     * @param configuration The configuration containing model settings
     * @param contextStore The shared context store for pipeline components
     */
    public OpenAiEmbeddingCreator(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore, configuration.argumentAsString("model", "text-embedding-ada-002"), THREADS);
    }

    /**
     * Creates an OpenAI embedding model instance with the specified parameters.
     * The method requires both the organization ID and API key to be set in the
     * environment variables.
     *
     * @param model The name of the OpenAI model to use
     * @param params Additional parameters (not used in this implementation)
     * @return A configured OpenAI embedding model instance
     * @throws IllegalStateException If either OPENAI_ORGANIZATION_ID or OPENAI_API_KEY environment variable is not set
     */
    @Override
    protected EmbeddingModel createEmbeddingModel(String model, String... params) {
        String openAiOrganizationId = Environment.getenv("OPENAI_ORGANIZATION_ID");
        String openAiApiKey = Environment.getenv("OPENAI_API_KEY");
        if (openAiOrganizationId == null || openAiApiKey == null) {
            throw new IllegalStateException("OPENAI_ORGANIZATION_ID or OPENAI_API_KEY environment variable not set");
        }
        return new OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder()
                .modelName(model)
                .organizationId(openAiOrganizationId)
                .apiKey(openAiApiKey)
                .maxRetries(0)
                .build();
    }
}
