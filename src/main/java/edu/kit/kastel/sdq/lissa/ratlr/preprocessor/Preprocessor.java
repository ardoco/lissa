/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Knowledge;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.codegraph.ComponentPreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.json.JsonSplitterArray;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.json.JsonConverterText;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.nl.SentenceInformation;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.nl.TemplateRequest;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.nl.TextSplitterListing;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text.TemplateReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier.CONFIG_NAME_SEPARATOR;

/**
 * Abstract base class for preprocessors that extract elements from artifacts.
 * Preprocessors are responsible for breaking down artifacts into smaller, more
 * manageable elements that can be used for trace link analysis.
 * <p>
 * All preprocessors have access to a shared {@link edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore} via the protected {@code contextStore} field,
 * which is initialized in the constructor and available to all subclasses.
 * Subclasses should not duplicate context handling or Javadoc for the context parameter.
 * </p>
 * The class supports various types of preprocessors:
 * <ul>
 *     <li>sentence: Breaks down text into sentences</li>
 *     <li>code: Processes source code in different ways:
 *         <ul>
 *             <li>code_chunking: Splits code into chunks</li>
 *             <li>code_method: Extracts methods from code</li>
 *             <li>code_tree: Processes code using a tree structure</li>
 *         </ul>
 *     </li>
 *     <li>model: Processes model artifacts:
 *         <ul>
 *             <li>model_uml: Processes UML models</li>
 *         </ul>
 *     </li>
 *     <li>summarize: Creates summaries of artifacts</li>
 *     <li>artifact: Processes single artifacts without breaking them down</li>
 * </ul>
 *
 * Each preprocessor type is created based on the module configuration and
 * implements its own strategy for extracting elements from artifacts.
 * 
 * @param <T> The type of knowledge that this preprocessor accepts
 */
public abstract class Preprocessor<T extends Knowledge> {
    /** Separator used in element identifiers */
    public static final String SEPARATOR = "$";

    /** Logger instance for this preprocessor */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The shared context store for pipeline components.
     * Available to all subclasses for accessing shared context.
     */
    protected final ContextStore contextStore;

    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected Preprocessor(ContextStore contextStore) {
        this.contextStore = Objects.requireNonNull(contextStore);
    }

    /**
     * Preprocesses a list of artifacts to extract elements.
     * The specific extraction strategy is implemented by each preprocessor subclass.
     *
     * @param artifacts The list of artifacts to preprocess
     * @return A list of elements extracted from the artifacts
     */
    public abstract List<Element> preprocess(List<T> artifacts);

    public static Preprocessor<Artifact> createPreprocessors(List<ModuleConfiguration> configurations, ContextStore contextStore) {
        return new PipelinePreprocessor(configurations, contextStore);
    }

    /**
     * Creates a preprocessor instance based on the module configuration.
     * The type of preprocessor is determined by the first part of the configuration name
     * (before the separator) and, for some types, the full configuration name.
     *
     * @param configuration The module configuration specifying the type of preprocessor
     * @param contextStore The shared context store for pipeline components
     * @return A new preprocessor instance
     * @throws IllegalArgumentException if the preprocessor name is not supported
     * @throws IllegalStateException if the configuration name is not recognized
     */
    static Preprocessor<Artifact> createPreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name()) {
            case "code_tree" -> new CodeTreePreprocessor(configuration, contextStore);
            case "artifact" -> new SingleArtifactPreprocessor(contextStore);
            case "code_graph_component" -> new ComponentPreprocessor(contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    /**
     * Creates a preprocessor instance based on the module configuration.
     * The type of preprocessor is determined by the first part of the configuration name
     * (before the separator) and, for some types, the full configuration name.
     *
     * @param configuration The module configuration specifying the type of preprocessor
     * @param contextStore The shared context store for pipeline components
     * @return A new preprocessor instance
     * @throws IllegalArgumentException if the preprocessor name is not supported
     * @throws IllegalStateException if the configuration name is not recognized
     */
    static Preprocessor<Element> createElementPreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case "json" -> switch (configuration.name()) {
                case "json_splitter_array" -> new JsonSplitterArray(configuration, contextStore);
                case "json_converter_text" -> new JsonConverterText(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "text" -> switch (configuration.name()) {
                case "text_splitter_listing" -> new TextSplitterListing(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "template" -> switch (configuration.name()) {
                case "template_replace" -> new TemplateReplacer(configuration, contextStore);
                case "template_openai" -> new TemplateRequest(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "context" -> switch (configuration.name()) {
                case "context_writer" -> new ContextWriter(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "sentence" -> switch (configuration.name()) {
                case "sentence" -> new SentencePreprocessor(configuration, contextStore);
                case "sentence_openai" -> new SentenceInformation(configuration, contextStore);
                default ->
                        throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "code" -> switch (configuration.name()) {
                case "code_chunking" -> new CodeChunkingPreprocessor(configuration, contextStore);
                case "code_method" -> new CodeMethodPreprocessor(configuration, contextStore);
                default ->
                        throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "model" -> switch (configuration.name()) {
                case "model_uml" -> new ModelUMLPreprocessor(configuration, contextStore);
                default ->
                        throw new IllegalArgumentException("Unsupported preprocessor name: " + configuration.name());
            };
            case "summarize" -> new SummarizePreprocessor(configuration, contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
    
}
