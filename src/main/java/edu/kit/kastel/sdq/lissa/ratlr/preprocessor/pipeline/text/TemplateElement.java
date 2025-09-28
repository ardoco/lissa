package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.SerializedContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ContextReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ElementReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.TemplateFormatter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

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
 *      This class introduces placeholders that retrieve information from the {@link ContextStore}, in addition to placeholders defined by {@link ElementReplacementRetriever}.
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
 * @see ElementReplacementRetriever
 */
public class TemplateElement extends SingleElementProcessingStage {
    
    private final AtomicReference<Element> elementReference = new AtomicReference<>();
    private final TemplateFormatter formatter;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param configuration The module configuration containing the placeholder format and template
     * @param contextStore The shared context store for pipeline components
     */
    public TemplateElement(ModuleConfiguration configuration, Function<ReplacementRetriever, ReplacementRetriever> retrieverProvider, ContextStore contextStore) {
        super(contextStore);
        ReplacementRetriever retriever = new ContextReplacementRetriever(new ElementReplacementRetriever(null, elementReference), contextStore);
        this.formatter = new TemplateFormatter(configuration, retrieverProvider.apply(retriever));
    }

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param configuration The module configuration containing the placeholder format and template
     * @param contextStore The shared context store for pipeline components
     */
    public TemplateElement(ModuleConfiguration configuration, ContextStore contextStore) {
        this(configuration, Function.identity(), contextStore);
    }

    public TemplateFormatter getFormatter() {
        return formatter;
    }

    /**
     * Preprocesses an element by iteratively matching and replacing the placeholders in the {@link #template}.
     * 
     * @param element the element currently being processed
     * @return a singleton element containing the replaced template as its content
     */
    @Override
    public List<Element> process(Element element) {
        elementReference.set(element);
        return List.of(Element.fromParent(element, formatter.format()));
    }
}
