package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.SerializedContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

import java.util.List;
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
 *     To maintain the pipeline convention, the provided elements are <b>not</b> preserved in the returned list.
 *     Each provided element is processed by replacing all placeholders of the template with its information when keyed.
 *     If the template does not contain any specified placeholder, then all children will contain the raw template as their content.
 * </p>
 *
 * <p>
 *      This class introduces placeholders that retrieve information from the {@link ContextStore}, in addition to placeholders defined by {@link ElementFormatter}.
 *      Their identifier must be prefixed with {@code context_} to indicate the intention of such access.
 *      Furthermore, the context to be retrieved must be an instance of {@link SerializedContext}.
 *      Hence, the information that replaces these placeholders is the result of {@link SerializedContext#asString()}.
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
 * @see ElementFormatter
 */
public class TemplateReplacer extends ElementFormatter {
    /**
     * The pattern defining an unnamed capturing group for a key used in the context store.
     */
    private static final Pattern CONTEXT_KEY_PATTERN = Pattern.compile("context_(.*)");
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
        super(configuration, contextStore);
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
        return List.of(Element.fromParent(element, replace(this.template, element)));
    }

    /**
     * Retrieves the string that replaces the placeholder.
     *
     * @param element the element that is the parent of this text replacement
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    protected String retrieveReplacement(Element element, String placeholderKey) {
        String replacement = super.retrieveReplacement(element, placeholderKey);
        if (replacement != null) {
            return replacement;
        }

        Matcher contextKeyMatcher = CONTEXT_KEY_PATTERN.matcher(placeholderKey);
        if (contextKeyMatcher.matches()) {
            String contextKey = contextKeyMatcher.group(1);
            if (!this.contextStore.hasContext(contextKey)) {
                throw new RuntimeException("%s: context store does not contain key '%s'".formatted(element.getIdentifier(), contextKey));
            }
            return this.contextStore.getContext(contextKey, SerializedContext.class).asString();
        }
        return null;
    }
}
