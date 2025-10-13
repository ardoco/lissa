package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class TextFormatter implements ValueFormatter<String> {

    /**
     * The string format for the placeholders.
     */
    private final String placeholderFormat;
    private final AtomicReference<String> textReference = new AtomicReference<>();
    private ReplacementRetriever retriever;
    private final String[] placeholderBounding;

    /**
     * 
     * @param configuration
     * @param retriever the retriever to be used to resolve placeholders
     */
    protected TextFormatter(ModuleConfiguration configuration, ReplacementRetriever retriever) {
        this.placeholderFormat = configuration.argumentAsString("placeholder", "<<<%s>>>");
        this.placeholderBounding = this.placeholderFormat.split("%s");
        this.retriever = retriever;
    }

    @Override
    public void addRetriever(Function<ReplacementRetriever, ReplacementRetriever> retrieverProvider) {
        retriever = retrieverProvider.apply(retriever);
    }

    @Override
    public void setValue(String value) {
        textReference.set(value);
    }

    /**
     * Replaces all placeholders in a text.
     *
     * @return the provided text with all placeholders being replaced
     */
    @Override
    public String format() {
        String text = textReference.get();

        int offset = 0;
        int rightBounding;
        int leftBounding;
        do {
            rightBounding = text.indexOf(placeholderBounding[1], offset);
            if (rightBounding == -1) {
                return text;
            }
            leftBounding = text.indexOf(placeholderBounding[0], offset, rightBounding);
            offset = rightBounding + placeholderBounding[1].length();
            if (leftBounding != -1) {
                String placeholderKey = text.substring(leftBounding + placeholderBounding[0].length(), rightBounding);
                String replacement = this.retriever.retrieveReplacement(placeholderKey);
                if (replacement == null) {
                    throw new RuntimeException("no replacement found for placeholder '%s'".formatted(placeholderKey));
                }

                text = text.replaceFirst("\\Q" + this.placeholderFormat.formatted(placeholderKey) + "\\E", replacement);
            }
        } while (leftBounding != -1);
        
        return text;
    }
}
