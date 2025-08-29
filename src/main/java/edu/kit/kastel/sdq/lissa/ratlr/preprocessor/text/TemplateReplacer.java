package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleElementPreprocessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A preprocessor that uses a template string with variable placeholders to generate diverse content.
 * This preprocessor is pipelineable, hence adapting the hierarchical structure of elements where:
 * <ul>
 *     <li>The provided elements represent arbitrary parent elements</li>
 *     <li>Child elements contain the replaced template as content (granularity level of parent incremented by 1)</li>
 * </ul>
 *
 * <p>
 *     The preprocessor uses a placeholder format to identify placeholders in the template.
 *     Each placeholder will be replaced by content using the contained key.
 *     The keys {@code element_content},
 *     {@code element_type},
 *     {@code element_identifier},
 *     {@code element_granularity},
 *     {@code element_parent},
 *     {@code element_parentId},
 *     {@code element_compare} return the corresponding serialized value of the element currently being processed.
 *     Keys starting with {@code context_} are considered to reference subsequent context identifiers in the provided context store.
 *     A placeholder key must always be resolvable to yield a replacement when invoking {@link #retrieveReplacement(Element, String)}.
 *     Subclasses overriding this method must obey this convention, however, it is not required, though recommended, to call the super method
 *     to retain resolving information of the element and context store.
 * </p>
 * 
 * <p>
 *     To maintain the pipeline convention, the provided elements are <b>not</b> preserved in the returned list.
 *     Each provided element is processed by replacing all placeholders of the template with its information when keyed.
 *     If the template does not contain any specified placeholder, then all children will contain the raw template as their content.
 * </p>
 *
 * <p>
 *     Configuration options:
 *     <ul>
 *         <li>{@code placeholder}: The string format of a placeholder, containing exactly one {@code %s} representing the identification key surrounded by arbitrary symbols</li>
 *         <li>{@code template}: The template used as base for all child elements, optionally containing the placeholders in the specified format with resolvable identification keys</li>
 *     </ul>
 * </p>
 * 
 * <p>
 *     Each replaced text element:
 *     <ul>
 *         <li>Has a unique identifier combining the parent element ID and index 0</li>
 *         <li>Maintains the same type as the parent element</li>
 *         <li>Contains only the replaced text as its content</li>
 *         <li>Has granularity level of the parent element plus 1</li>
 *         <li>Is marked for comparison (compare=true)</li>
 *     </ul>
 * </p>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class TemplateReplacer extends SingleElementPreprocessor {

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
     * The pattern defining an unnamed capturing group for a key used in the context store.
     */
    private static final Pattern CONTEXT_KEY_PATTERN = Pattern.compile("context_(.*)");

    /**
     * The string format for the placeholders.
     */
    private final String placeholderFormat;
    /**
     * The regex pattern defining an unnamed capturing group for a single placeholder across the whole template.
     * E.g.: {@code .*\Q<<<\E(.*)\Q>>>\E.*}
     */
    private final Pattern placeholderPattern;
    /**
     * The base template used for all elements.
     */
    private final String template;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param configuration The module configuration containing the placeholder format and template
     * @param contextStore The shared context store for pipeline components
     */
    public TemplateReplacer(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.placeholderFormat = configuration.argumentAsString("placeholder", "<<<%s>>>");
        this.placeholderPattern = Pattern.compile(".*" + createCapturingGroupFromReplaceFormat(this.placeholderFormat) + ".*");
        this.template = configuration.argumentAsString("template");
    }

    /**
     * Preprocesses an element by iteratively matching and replacing the placeholders in the {@link #template}.
     * 
     * @param element the element currently being processed
     * @return a singleton element containing the replaced template as its content
     */
    @Override
    public List<Element> preprocess(Element element) {
        String text = this.template;
        Matcher placeholderMatcher = this.placeholderPattern.matcher(text);
        while (placeholderMatcher.matches()) {
            String placeholderKey = placeholderMatcher.group(1);
            String replacement = retrieveReplacement(element, placeholderKey);
            if (replacement == null) {
                throw new RuntimeException("%s: no replacement found for placeholder '%s'".formatted(element.getIdentifier(), placeholderKey));
            }

            text = text.replace(this.placeholderFormat.formatted(placeholderKey), replacement);

            placeholderMatcher = this.placeholderPattern.matcher(text);
        }
        return List.of(Element.fromParent(element, text));
    }

    /**
     * Retrieves the string that replaces the placeholder.
     * 
     * @param element the element that is the parent of this text replacement
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    protected String retrieveReplacement(Element element, String placeholderKey) {
        Matcher contextKeyMatcher = CONTEXT_KEY_PATTERN.matcher(placeholderKey);
        if (contextKeyMatcher.matches()) {
            String contextKey = contextKeyMatcher.group();
            if (!this.contextStore.hasContext(contextKey)) {
                throw new RuntimeException("%s: context store does not contain key '%s'".formatted(element.getIdentifier(), contextKey));
            }
            return this.contextStore.getSerializedContext(placeholderKey);
        } else if (ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY.containsKey(placeholderKey)) {
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
