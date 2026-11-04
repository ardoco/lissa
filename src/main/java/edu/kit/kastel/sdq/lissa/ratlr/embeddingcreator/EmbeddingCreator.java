/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.util.List;
import java.util.Objects;

import edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingConfiguration;
import edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingPlatform;
import edu.kit.kastel.mcse.ardoco.llm.util.EnvironmentProvider;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Adapts LiSSA's {@link Element}-based embedding usage and {@link ModuleConfiguration} to the
 * framework-neutral {@link edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingCreator} of the
 * {@code llm-access} library. Embeddings are computed on the {@linkplain Element#getContent() element
 * content}; caching and token-length handling are provided by the library.
 */
public class EmbeddingCreator {

    private final edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingCreator delegate;

    private EmbeddingCreator(edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingCreator delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * Calculates the embedding for a single element.
     *
     * @param element The element to embed
     * @return The vector embedding of the element's content
     */
    public float[] calculateEmbedding(Element element) {
        return delegate.calculateEmbedding(element.getContent());
    }

    /**
     * Calculates embeddings for a list of elements.
     *
     * @param elements The elements to embed
     * @return A list of vector embeddings, in the same order as the input elements
     */
    public List<float[]> calculateEmbeddings(List<Element> elements) {
        return delegate.calculateEmbeddings(
                elements.stream().map(Element::getContent).toList());
    }

    /**
     * Creates an embedding creator based on the provided configuration.
     * The type of creator is determined by the configuration's name field. API keys and hosts are read from the
     * {@linkplain ContextStore#environment() environment of the context store}; embeddings are cached via the
     * default {@link edu.kit.kastel.mcse.ardoco.llm.cache.CacheManager} (not needed for the mock creator).
     *
     * @param configuration The configuration specifying the embedding creator
     * @param contextStore The shared context store for pipeline components, providing the environment
     * @return An embedding creator adapter
     */
    public static EmbeddingCreator createEmbeddingCreator(
            ModuleConfiguration configuration, ContextStore contextStore) {
        EmbeddingConfiguration embeddingConfiguration =
                toEmbeddingConfiguration(configuration, contextStore.environment());
        return new EmbeddingCreator(
                edu.kit.kastel.mcse.ardoco.llm.embedding.EmbeddingCreator.create(embeddingConfiguration));
    }

    private static EmbeddingConfiguration toEmbeddingConfiguration(
            ModuleConfiguration configuration, EnvironmentProvider environment) {
        return switch (configuration.name()) {
            case "ollama" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.OLLAMA)
                        .modelName(configuration.argumentAsString("model", "nomic-embed-text:v1.5"))
                        .environment(environment)
                        .build();
            case "openai" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.OPENAI)
                        .modelName(configuration.argumentAsString("model", "text-embedding-ada-002"))
                        .environment(environment)
                        .build();
            case "onnx" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.ONNX)
                        .modelName(configuration.argumentAsString("model"))
                        .pathToModel(configuration.argumentAsString("path_to_model"))
                        .pathToTokenizer(configuration.argumentAsString("path_to_tokenizer"))
                        .environment(environment)
                        .build();
            case "openwebui" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.OPENWEBUI)
                        .modelName(configuration.argumentAsString("model", "nomic-embed-text:v1.5"))
                        .environment(environment)
                        .build();
            // Bifrost model names are provider-prefixed (e.g. "openai/text-embedding-3-large").
            case "bifrost" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.BIFROST)
                        .modelName(configuration.argumentAsString("model", "openai/text-embedding-3-large"))
                        .environment(environment)
                        .build();
            // The mock creator ignores the model, so do not read a "model" argument here: that way a stray
            // "model" on a mock configuration is reported as an unread (misconfigured) parameter.
            case "mock" ->
                EmbeddingConfiguration.builder(EmbeddingPlatform.MOCK)
                        .environment(environment)
                        .build();
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
}
