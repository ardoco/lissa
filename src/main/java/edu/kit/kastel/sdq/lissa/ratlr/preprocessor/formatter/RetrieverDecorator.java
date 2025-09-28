package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

public class RetrieverDecorator implements ReplacementRetriever {
    
    private final ReplacementRetriever retriever;

    protected RetrieverDecorator(ReplacementRetriever retriever) {
        this.retriever = retriever;
    }

    @Override
    public String retrieveReplacement(String placeholderKey) {
        return retriever == null ? null : retriever.retrieveReplacement(placeholderKey);
    }
}
