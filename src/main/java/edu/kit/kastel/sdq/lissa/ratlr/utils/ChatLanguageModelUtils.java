/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.cache.Cache;
import edu.kit.kastel.sdq.lissa.ratlr.cache.classifier.ClassifierCacheKey;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;

import dev.langchain4j.model.chat.ChatModel;

public final class ChatLanguageModelUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatLanguageModelUtils.class);

    private ChatLanguageModelUtils() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Sends multiple requests to the language model and caches the responses.
     *
     * @param request The request to send to the language model
     * @param provider The chat language model provider
     * @param llm The chat model instance
     * @param cache The cache instance to use for caching responses
     * @param numberOfRequests The positive natural number of requests to send to the language model
     * @return A list of replies from the language model
     * @throws IllegalArgumentException If the number of requests is less than 1
     */
    @NotNull
    public static List<String> nCachedRequest(
            String request, ChatLanguageModelProvider provider, ChatModel llm, Cache cache, int numberOfRequests) {
        if (numberOfRequests < 1) {
            throw new IllegalArgumentException("Number of requests must be at least 1");
        }
        ClassifierCacheKey cacheKey =
                ClassifierCacheKey.of(provider.cacheParameters(), numberOfRequests + " results: \n" + request);

        LOGGER.debug(
                "Cache lookup for key: model={}, seed={}, temp={}, mode=CHAT",
                provider.cacheParameters().modelName(),
                provider.cacheParameters().seed(),
                provider.cacheParameters().temperature());
        LOGGER.debug("Request hash: {}", Integer.toHexString(request.hashCode()));

        List<String> responses = cache.get(cacheKey, List.class);
        if (responses == null || responses.size() < numberOfRequests) {
            LOGGER.debug("CACHE MISS - Making {} new LLM request(s)", numberOfRequests);
            responses = new ArrayList<>();
            LOGGER.info("Optimizing ({}) with {} requests", provider.modelName(), numberOfRequests);
            for (int i = 1; i <= numberOfRequests; i++) {
                LOGGER.debug("  Sending LLM request {}/{}", i, numberOfRequests);
                String response = llm.chat(request);
                LOGGER.debug("  Received response {}/{} (length: {} chars)", i, numberOfRequests, response.length());
                responses.add(response);
            }
            cache.put(cacheKey, responses);
            LOGGER.debug("Cached {} response(s) for future use", numberOfRequests);
        } else {
            LOGGER.debug("CACHE HIT - Retrieved {} response(s) from cache", responses.size());
        }
        LOGGER.debug("Responses: {}", responses);
        return responses;
    }

    /**
     * A wrapper for sending a single cached request to the language model using the
     * {@link #nCachedRequest(String, ChatLanguageModelProvider, ChatModel, Cache, int)} method.
     *
     * @param request The request to send to the language model
     * @param provider The chat language model provider
     * @param llm The chat model instance
     * @param cache The cache instance to use for caching responses
     * @return The reply from the language model
     */
    public static String cachedRequest(String request, ChatLanguageModelProvider provider, ChatModel llm, Cache cache) {
        return nCachedRequest(request, provider, llm, cache, 1).getFirst();
    }
}
