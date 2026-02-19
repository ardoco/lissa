/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * A chat model that initializes its delegate lazily using a supplier.
 * This allows for deferred creation of the underlying chat model until it is actually needed.
 * The delegate is initialized in a thread-safe manner, ensuring that only one instance is created
 * even if multiple threads access the model concurrently.
 */
public final class LazyChatModel implements ChatModel {

    private final Supplier<ChatModel> delegateSupplier;

    private @Nullable ChatModel delegate;

    /**
     * Creates a new LazyChatModel with the specified supplier for the delegate.
     *
     * @param delegateSupplier The supplier that provides the chat model delegate
     */
    public LazyChatModel(Supplier<ChatModel> delegateSupplier) {
        this.delegateSupplier = Objects.requireNonNull(delegateSupplier);
    }

    private ChatModel delegate() {
        if (delegate != null) {
            return delegate;
        }
        synchronized (this) {
            if (delegate == null) {
                delegate = delegateSupplier.get();
            }
            return delegate;
        }
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        return delegate().chat(chatRequest);
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        return delegate().doChat(chatRequest);
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return delegate().defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return delegate().listeners();
    }

    @Override
    public ModelProvider provider() {
        return delegate().provider();
    }

    @Override
    public String chat(String userMessage) {
        return delegate().chat(userMessage);
    }

    @Override
    public ChatResponse chat(ChatMessage... messages) {
        return delegate().chat(messages);
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        return delegate().chat(messages);
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return delegate().supportedCapabilities();
    }
}
