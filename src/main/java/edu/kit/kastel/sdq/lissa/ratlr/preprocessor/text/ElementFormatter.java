package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleElementPreprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An abstract preprocessor that offers to define variable placeholders and replacing them in texts.
 *
 * <p>
 *     Identifying placeholders is done by using a specifiable format.
 *     Each placeholder will be replaced by content using the contained key.
 *     The keys {@code element_content},
 *     {@code element_type},
 *     {@code element_identifier},
 *     {@code element_granularity},
 *     {@code element_parent},
 *     {@code element_parentId},
 *     {@code element_compare} return the corresponding serialized value of a provided element.
 *     A placeholder key must always be resolvable to yield a replacement when invoking {@link #retrieveReplacement(Element, String)}.
 *     Subclasses overriding this method must obey this convention; however, it is not required, though recommended, to call the super method
 *     to retain resolving information of the element.
 * </p>
 *
 * <p>
 *     Configuration options:
 *     <ul>
 *         <li>{@code placeholder}: The string format of a placeholder, containing exactly one {@code %s} representing the identification key surrounded by arbitrary symbols</li>
 *     </ul>
 * </p>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public abstract class ElementFormatter extends SingleElementPreprocessor {

    /**
     * The keying of element information.
     */
    private static final Map<String, Function<Element, String>> ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY = new HashMap<>() {{
        put("element_content", Element::getContent);
        put("element_type", Element::getType);
        put("element_identifier", Element::getIdentifier);
        put("element_granularity", element -> String.valueOf(element.getGranularity()));
        put("element_parent", element -> String.valueOf(element.getParent()));
        put("element_parentId", element -> element.getParent().getIdentifier());
        put("element_compare", element -> String.valueOf(element.isCompare()));
    }};

    /**
     * The string format for the placeholders.
     */
    private final String placeholderFormat;
    /**
     * The regex pattern defining an unnamed capturing group for a single placeholder across the whole template.
     * E.g.: {@code .*\Q<<<\E(.*)\Q>>>\E.*}
     */
    private final Pattern placeholderPattern;

    public ElementFormatter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.placeholderFormat = configuration.argumentAsString("placeholder", "<<<%s>>>");
        this.placeholderPattern = Pattern.compile("(.|\n)*" + createCapturingGroupFromReplaceFormat(this.placeholderFormat) + "(.|\n)*");
    }

    /**
     * Replaces all placeholders in a text.
     * 
     * @param text the text containing placeholders
     * @param element the element to retrieve information from
     * @return the provided text with all placeholders being replaced
     */
    protected final String replace(String text, Element element) {
        Map<String, String> contentByPlaceholder = new HashMap<>();
        String placeholderText = text;
        Matcher placeholderMatcher = this.placeholderPattern.matcher(placeholderText);
        while (placeholderMatcher.matches()) {
            String placeholderKey = placeholderMatcher.group(2);
            String replacement = retrieveReplacement(element, placeholderKey);
            if (replacement == null) {
                throw new RuntimeException("%s: no replacement found for placeholder '%s'".formatted(element.getIdentifier(), placeholderKey));
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
     * Retrieves the string that replaces the placeholder.
     *
     * @param element the element that is the parent of this text replacement
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    protected String retrieveReplacement(Element element, String placeholderKey) {
        if (ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY.containsKey(placeholderKey)) {
            return ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY.get(placeholderKey).apply(element);
        }
        return null;
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
