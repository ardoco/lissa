/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.embeddingcreator;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;

import edu.kit.kastel.sdq.lissa.ratlr.cache.*;
import edu.kit.kastel.sdq.lissa.ratlr.cache.embedding.EmbeddingCacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.cache.embedding.EmbeddingCacheParameter;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Futures;

import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * Abstract base class for embedding creators that implement caching functionality.
 * This class provides a framework for creating and caching embeddings with support for:
 * <ul>
 *     <li>Multi-threaded embedding generation</li>
 *     <li>Automatic caching of embeddings to improve performance</li>
 *     <li>Handling of long texts through token length management</li>
 *     <li>Fallback mechanisms for failed embedding generation</li>
 * </ul>
 *
 * The class uses a cache to store previously generated embeddings and implements
 * a sophisticated mechanism to handle texts that exceed the maximum token length
 * of the underlying embedding model.
 */
abstract class CachedEmbeddingCreator extends EmbeddingCreator {
    // TODO Handle Token Length better .. 8192 is the length for ada
    private static final int MAX_TOKEN_LENGTH = 8000;

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(CachedEmbeddingCreator.class);
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Cache<EmbeddingCacheKey> cache;
    private final EmbeddingModel embeddingModel;
    private final String rawNameOfModel;
    private final int threads;
    private final EmbeddingCacheParameter embeddingCacheParameter;

    /**
     * Creates a new cached embedding creator with the specified model and thread count.
     *
     * @param contextStore The shared context store for pipeline components
     * @param model The name of the embedding model to use
     * @param threads The number of threads to use for parallel embedding generation
     * @param params Additional parameters for the embedding model
     */
    protected CachedEmbeddingCreator(ContextStore contextStore, String model, int threads, String... params) {
        super(contextStore);
        this.embeddingCacheParameter = new EmbeddingCacheParameter(model);
        this.cache = CacheManager.getDefaultInstance().getCache(this, embeddingCacheParameter);
        this.embeddingModel = Objects.requireNonNull(createEmbeddingModel(model, params));
        this.rawNameOfModel = model;
        this.threads = Math.max(1, threads);
    }

    /**
     * Creates an instance of the embedding model with the specified parameters.
     * This method must be implemented by concrete subclasses to provide the actual
     * model creation logic.
     *
     * @param model The name of the model to create
     * @param params Additional parameters for model creation
     * @return A new instance of the embedding model
     */
    protected abstract EmbeddingModel createEmbeddingModel(String model, String... params);

    /**
     * Calculates embeddings for a list of elements, using either sequential or parallel processing
     * based on the configured thread count.
     *
     * @param elements The list of elements to create embeddings for
     * @return A list of vector embeddings, in the same order as the input elements
     */
    @Override
    public final List<float[]> calculateEmbeddings(List<Element> elements) {
        if (threads == 1) return calculateEmbeddingsSequential(elements);

        int threadCount = Math.min(threads, elements.size());
        int numberOfElementsPerThread = elements.size() / threadCount;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<List<float[]>>> futureResults = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            int start = i * numberOfElementsPerThread;
            int end = i == threadCount - 1 ? elements.size() : (i + 1) * numberOfElementsPerThread;
            List<Element> subList = elements.subList(start, end);
            futureResults.add(executor.submit(() -> {
                var embeddingModelInstance = createEmbeddingModel(this.rawNameOfModel);
                return calculateEmbeddingsSequential(embeddingModelInstance, subList);
            }));
        }
        logger.info("Waiting for classification to finish. Elements in queue: {}", futureResults.size());

        try {
            executor.shutdown();
            boolean success = executor.awaitTermination(1, TimeUnit.DAYS);
            if (!success) {
                logger.error("Embedding did not finish in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        executor.close();

        return futureResults.stream()
                .map(f -> Futures.getLogged(f, logger))
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Calculates embeddings sequentially using the default embedding model.
     *
     * @param elements The list of elements to create embeddings for
     * @return A list of vector embeddings
     */
    private List<float[]> calculateEmbeddingsSequential(List<Element> elements) {
        return this.calculateEmbeddingsSequential(this.embeddingModel, elements);
    }

    /**
     * Calculates embeddings sequentially using the specified embedding model.
     *
     * @param embeddingModel The model to use for embedding generation
     * @param elements The list of elements to create embeddings for
     * @return A list of vector embeddings
     */
    private List<float[]> calculateEmbeddingsSequential(EmbeddingModel embeddingModel, List<Element> elements) {
        List<float[]> embeddings = new ArrayList<>();
        for (Element element : elements) {
            embeddings.add(calculateFinalEmbedding(embeddingModel, cache, embeddingCacheParameter, element));
        }
        return embeddings;
    }

    /**
     * Calculates the final embedding for an element, using the cache if available.
     * This method implements a sophisticated caching and error handling strategy:
     * <ol>
     *     <li>First, generates a unique cache key based on the element's content</li>
     *     <li>Checks if a cached embedding exists for this key</li>
     *     <li>If cached, returns the existing embedding immediately</li>
     *     <li>If not cached:
     *         <ul>
     *             <li>Attempts to generate a new embedding using the provided model</li>
     *             <li>If successful, caches the result and returns it</li>
     *             <li>If generation fails (e.g., due to token length), attempts to fix the issue
     *                 using {@link #tryToFixWithLength}</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * The method uses a composite cache key that includes:
     * <ul>
     *     <li>The model name</li>
     *     <li>The operation mode (EMBEDDING)</li>
     *     <li>The original content</li>
     *     <li>A generated key based on the content</li>
     * </ul>
     *
     * @param embeddingModel The model to use for embedding generation
     * @param cache The cache to use for storing and retrieving embeddings
     * @param embeddingCacheParameter The EmbeddingCacheParameter of the model being used
     * @param element The element to create an embedding for
     * @return The vector embedding of the element, either from cache or newly generated
     */
    private static float[] calculateFinalEmbedding(
            EmbeddingModel embeddingModel,
            Cache<EmbeddingCacheKey> cache,
            EmbeddingCacheParameter embeddingCacheParameter,
            Element element) {

        String elementContent = element.getContent();
        float[] cachedEmbedding = cache.get(elementContent, float[].class);
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        } else {
            STATIC_LOGGER.info("Calculating embedding for: {}", element.getIdentifier());
            try {
                float[] embedding =
                        embeddingModel.embed(elementContent).content().vector();
                cache.put(elementContent, embedding);
                return embedding;
            } catch (Exception e) {
                STATIC_LOGGER.error(
                        "Error while calculating embedding for .. try to fix ..: {}", element.getIdentifier());
                // Probably the length was too long .. check that
                return tryToFixWithLength(
                        embeddingModel,
                        cache,
                        embeddingCacheParameter.modelName(),
                        elementContent,
                        element.getContent());
            }
        }
    }

    /**
     * Attempts to fix embedding generation for content that exceeds the maximum token length.
     * This method uses binary search to find the maximum content length that fits within
     * the token limit and generates an embedding for that truncated content.
     *
     * @param embeddingModel The model to use for embedding generation
     * @param cache The cache to use for storing and retrieving embeddings
     * @param rawNameOfModel The name of the model being used
     * @param elementContent The original element content
     * @param content The content that exceeded the token limit
     * @return The vector embedding of the truncated content
     * @throws IllegalArgumentException If the token length was not the cause of the failure
     */
    private static float[] tryToFixWithLength(
            EmbeddingModel embeddingModel,
            Cache<EmbeddingCacheKey> cache,
            String rawNameOfModel,
            String elementContent,
            String content) {
        EmbeddingCacheKey originalKey = cache.getCacheParameter().createCacheKey(elementContent);
        String newKey = originalKey.localKey() + "_fixed_" + MAX_TOKEN_LENGTH;

        // We need the old keys for backwards compatibility
        @SuppressWarnings("deprecation")
        EmbeddingCacheKey newCacheKey =
                EmbeddingCacheKey.ofRaw(rawNameOfModel, "(FIXED::%d): %s".formatted(MAX_TOKEN_LENGTH, content), newKey);

        @SuppressWarnings("deprecation")
        float[] cachedEmbedding = cache.getViaInternalKey(newCacheKey, float[].class);
        if (cachedEmbedding != null) {
            if (STATIC_LOGGER.isInfoEnabled()) {
                STATIC_LOGGER.info("using fixed embedding for: {}", originalKey.localKey());
            }
            return cachedEmbedding;
        }
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding encoding = registry.getEncodingForModel(rawNameOfModel)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Embedding Model. Don't know how to handle previous exception"));
        int tokens = encoding.countTokens(content);
        if (tokens < MAX_TOKEN_LENGTH)
            throw new IllegalArgumentException(
                    "Token length was not too long. Don't know how to handle previous exception");

        // Binary search for max length of string
        int left = 0;
        int right = content.length();
        while (left < right) {
            int mid = left + (right - left) / 2;
            String subContent = content.substring(0, mid);
            int subTokens = encoding.countTokens(subContent);
            if (subTokens >= MAX_TOKEN_LENGTH) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        String fixedContent = content.substring(0, left);
        float[] embedding = embeddingModel.embed(fixedContent).content().vector();
        if (STATIC_LOGGER.isInfoEnabled()) {
            STATIC_LOGGER.info("using fixed embedding for: {}", originalKey.localKey());
        }
        cache.putViaInternalKey(newCacheKey, embedding);
        return embedding;
    }
}
