package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.SerializedContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContextReplacementRetriever extends RetrieverDecorator {
    
    /**
     * The pattern defining an unnamed capturing group for a key used in the context store.
     */
    private static final Pattern CONTEXT_KEY_PATTERN = Pattern.compile("context_(.*)");
    private final ContextStore contextStore;

    public ContextReplacementRetriever(ReplacementRetriever retriever, ContextStore contextStore) {
        super(retriever);
        this.contextStore = contextStore;
    }

    @Override
    public String retrieveReplacement(String placeholderKey) {
        String replacement = super.retrieveReplacement(placeholderKey);
        if (replacement != null) {
            return replacement;
        }

        Matcher contextKeyMatcher = CONTEXT_KEY_PATTERN.matcher(placeholderKey);
        if (contextKeyMatcher.matches()) {
            String contextKey = contextKeyMatcher.group(1);
            if (!this.contextStore.hasContext(contextKey)) {
                throw new RuntimeException("context store does not contain key '%s'".formatted(contextKey));
            }
            return this.contextStore.getContext(contextKey, SerializedContext.class).asString();
        }
        return null;
    }
}
