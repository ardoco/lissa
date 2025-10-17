package edu.kit.kastel.sdq.lissa.ratlr.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class CachedChatModel implements ChatModel {

    private static final ObjectMapper CUSTOM_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    private final ChatModel model;
    private final Cache cache;
    private final ChatLanguageModelProvider provider;

    public CachedChatModel(ChatLanguageModelProvider provider, Object origin) {
        this(provider, CacheManager.getDefaultInstance().getCache(origin, provider.getCacheParameters()));
    }

    public CachedChatModel(ChatLanguageModelProvider provider, Cache cache) {
        this.provider = provider;
        this.model = provider.createChatModel();
        this.cache = cache;
    }

    public CachedChatModel(ChatLanguageModelProvider provider, Object origin, ChatModel model) {
        this.provider = provider;
        this.model = model;
        this.cache = CacheManager.getDefaultInstance().getCache(origin, this.provider.getCacheParameters());
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        CacheKey cacheKey = getCacheKey(chatRequest);
        return getCachedResponse(cacheKey).map(CachedChatModel::convert)
                .orElse(chatAndCache(() -> model.chat(chatRequest), cacheKey, CachedChatModel::convert));
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return model.defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return model.listeners();
    }

    @Override
    public ModelProvider provider() {
        return model.provider();
    }

    @Override
    public String chat(String userMessage) {
        CacheKey cacheKey = getCacheKey(userMessage);
        return getCachedResponse(cacheKey)
                .orElse(chatAndCache(() -> model.chat(userMessage), cacheKey, Function.identity()));
    }

    @Override
    public ChatResponse chat(ChatMessage... messages) {
        CacheKey cacheKey = getCacheKey(messages);
        return getCachedResponse(cacheKey).map(CachedChatModel::convert)
                .orElse(chatAndCache(() -> model.chat(messages), cacheKey, CachedChatModel::convert));
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        CacheKey cacheKey = getCacheKey(messages);
        return getCachedResponse(cacheKey).map(CachedChatModel::convert)
                .orElse(chatAndCache(() -> model.chat(messages), cacheKey, CachedChatModel::convert));
    }

    private <T> T chatAndCache(Supplier<T> actualChat, CacheKey cacheKey, Function<T, String> responseConverter) {
        T chatResponse = actualChat.get();
        cache.put(cacheKey, responseConverter.apply(chatResponse));
        cache.flush();
        return chatResponse;
    }
    
    private static String convert(ChatResponse chatResponse) {
        return Jsons.writeValueAsString(chatResponse.toBuilder()
                .metadata(new ChatResponseMetadataWrapper(chatResponse.metadata().toBuilder()))
                .build(), CUSTOM_MAPPER);
    }
    
    private static ChatResponse convert(String cachedResponse) {
        return Jsons.readValue(cachedResponse, new TypeReference<ChatResponseWrapper>() {}, CUSTOM_MAPPER);
    }
    
    private Optional<String> getCachedResponse(CacheKey cacheKey) {
        String cachedResponse = cache.get(cacheKey, String.class);
        return cachedResponse == null ? Optional.empty() : Optional.of(cachedResponse);
    }
    
    private CacheKey getCacheKey(String message) {
        return CacheKey.of(provider.modelName(), provider.seed(), provider.temperature(), CacheKey.Mode.CHAT, message);
    }
    
    private <T> CacheKey getCacheKey(T request) {
        return CacheKey.of(provider.modelName(), provider.seed(), provider.temperature(), CacheKey.Mode.CHAT, Jsons.writeValueAsString(request, CUSTOM_MAPPER));
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return model.supportedCapabilities();
    }
    
    private static final class ChatResponseWrapper extends ChatResponse {

        ChatResponseWrapper(Builder builder) {
            super(builder);
        }
        
        @JsonCreator
        ChatResponseWrapper(@JsonProperty("aiMessage") AiMessageWrapper aiMessage, @JsonProperty("metadata") ChatResponseMetadataWrapper metadata) {
            this(new Builder().aiMessage(aiMessage).metadata(metadata));
        }
    }
    
    private static final class AiMessageWrapper extends AiMessage {
        @JsonCreator
        AiMessageWrapper(@JsonProperty("text") String text, @JsonProperty("thinking") String thinking, 
                         @JsonProperty("toolExecutionRequests") List<ToolExecutionRequestWrapper> toolExecutionRequests, 
                         @JsonProperty("attributes") Map<String, Object> attributes) {
            super(new Builder().text(text)
                    .thinking(thinking)
                    .toolExecutionRequests(toolExecutionRequests.stream().map(ToolExecutionRequestWrapper::convert).toList())
                    .attributes(attributes));
        }
    }
    
    private static final class ToolExecutionRequestWrapper {
        private String id;
        private String name;
        private String arguments;
        
        public ToolExecutionRequest convert() {
            return ToolExecutionRequest.builder().id(id).name(name).arguments(arguments).build();
        }
    }
    
    private static final class ChatResponseMetadataWrapper extends ChatResponseMetadata {
        ChatResponseMetadataWrapper(Builder<?> builder) {
            super(builder);
        }
        @JsonCreator
        ChatResponseMetadataWrapper(@JsonProperty("id") String id, @JsonProperty("modelName") String modelName, 
                                    @JsonProperty("tokenUsage") TokenUsage tokenUsage, @JsonProperty("finishReason") FinishReason finishReason) {
            this(new ChatResponseMetadata.Builder<>().id(id).modelName(modelName).tokenUsage(tokenUsage).finishReason(finishReason));
        }
    }
}
