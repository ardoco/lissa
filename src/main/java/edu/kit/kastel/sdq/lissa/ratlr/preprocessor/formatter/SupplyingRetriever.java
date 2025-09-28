package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import java.util.concurrent.atomic.AtomicReference;

public abstract class SupplyingRetriever<T> extends RetrieverDecorator {
    
    private final AtomicReference<T> supplier;
    
    protected SupplyingRetriever(ReplacementRetriever retriever, AtomicReference<T> valueReference) {
        super(retriever);
        this.supplier = valueReference;
    }

    @Override
    public String retrieveReplacement(String placeholderKey) {
        T value = supplier.get();
        String replacement = super.retrieveReplacement(placeholderKey);
        return replacement == null ? retrieveReplacement(value, placeholderKey) : replacement;
    }

    /**
     * Retrieves the string that replaces the placeholder.
     *
     * @param value the value to be used to retrieve information from
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    protected abstract String retrieveReplacement(T value, String placeholderKey);
}
