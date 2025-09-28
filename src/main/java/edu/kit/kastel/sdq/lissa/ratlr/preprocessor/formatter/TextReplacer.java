package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextReplacer {

    /**
     * The string format for the placeholders.
     */
    private final String placeholderFormat;
    /**
     * The regex pattern defining an unnamed capturing group for a single placeholder across the whole template.
     * E.g.: {@code .*\Q<<<\E(.*)\Q>>>\E.*}
     */
    private final Pattern placeholderPattern;
    private final ReplacementRetriever retriever;

    /**
     * 
     * @param configuration
     * @param retriever the retriever to be used to resolve placeholders
     */
    protected TextReplacer(ModuleConfiguration configuration, ReplacementRetriever retriever) {
        this.placeholderFormat = configuration.argumentAsString("placeholder", "<<<%s>>>");
        this.retriever = retriever;
        this.placeholderPattern = Pattern.compile("(.|\n)*" + createCapturingGroupFromReplaceFormat(this.placeholderFormat) + "(.|\n)*");
    }

    /**
     * Replaces all placeholders in a text.
     *
     * @param text the text containing placeholders
     * @return the provided text with all placeholders being replaced
     */
    protected final String replace(String text) {
        Map<String, String> contentByPlaceholder = new HashMap<>();
        String placeholderText = text;
        Matcher placeholderMatcher = this.placeholderPattern.matcher(placeholderText);
        while (placeholderMatcher.matches()) {
            String placeholderKey = placeholderMatcher.group(2);
            String replacement = this.retriever.retrieveReplacement(placeholderKey);
            if (replacement == null) {
                throw new RuntimeException("no replacement found for placeholder '%s'".formatted(placeholderKey));
            }

            contentByPlaceholder.put(this.placeholderFormat.formatted(placeholderKey), replacement);
            placeholderText = placeholderText.replace(this.placeholderFormat.formatted(placeholderKey), "");

            placeholderMatcher = this.placeholderPattern.matcher(placeholderText);
        }

        for (Map.Entry<String, String> replaceEntry : contentByPlaceholder.entrySet()) {
            text = text.replace(replaceEntry.getKey(), replaceEntry.getValue());
        }

        return text;
    }

    /**
     * Generates a regex unnamed capturing group for the given placeholder format.
     * Escapes all symbols surrounding the key placeholder in the format.
     *
     * @param placeholderFormat the string format of a placeholder
     * @return a regex with capturing group
     */
    private static String createCapturingGroupFromReplaceFormat(String placeholderFormat) {
        String delimiter = "%s";
        StringJoiner joiner = new StringJoiner("(" + delimiter + ")");
        for (String split : placeholderFormat.splitWithDelimiters(delimiter, -1)) {
            if (split.equals(delimiter)) {
                continue;
            }
            joiner.add("\\Q" + split + "\\E");
        }
        return joiner.toString().formatted(".*");
    }
}
