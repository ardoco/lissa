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
import java.util.Set;

public class CachedChatModel implements ChatModel {

    private static final ObjectMapper CUSTOM_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    private final ChatModel model;
    private final Cache cache;
    private final ChatLanguageModelProvider modelProvider;

    public CachedChatModel(ChatLanguageModelProvider modelProvider, Object origin) {
        this(modelProvider, CacheManager.getDefaultInstance().getCache(origin, modelProvider.getCacheParameters()));
    }

    public CachedChatModel(ChatLanguageModelProvider modelProvider, Cache cache) {
        this.modelProvider = modelProvider;
        this.model = modelProvider.createChatModel();
        this.cache = cache;
    }

    public CachedChatModel(ChatLanguageModelProvider provider, Object origin, ChatModel model) {
        this.modelProvider = provider;
        this.model = model;
        this.cache = CacheManager.getDefaultInstance().getCache(origin, modelProvider.getCacheParameters());
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        CacheKey cacheKey = CacheKey.of(
                modelProvider.modelName(), modelProvider.seed(), modelProvider.temperature(), CacheKey.Mode.CHAT, Jsons.writeValueAsString(chatRequest, CUSTOM_MAPPER));
        String cachedResponse = cache.get(cacheKey, String.class);
        if (cachedResponse != null) {
            return Jsons.readValue(cachedResponse, new TypeReference<ChatResponseWrapper>() {}, CUSTOM_MAPPER);
        }
        ChatResponse chatResponse = model.chat(chatRequest);
        cache.put(cacheKey, Jsons.writeValueAsString(chatResponse.toBuilder()
                .metadata(new ChatResponseMetadataWrapper(chatResponse.metadata().toBuilder()))
                .build(), CUSTOM_MAPPER));
        cache.flush();
        return chatResponse;
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
        return model.chat(userMessage);
    }

    @Override
    public ChatResponse chat(ChatMessage... messages) {
        return model.chat(messages);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        return model.chat(messages);
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
