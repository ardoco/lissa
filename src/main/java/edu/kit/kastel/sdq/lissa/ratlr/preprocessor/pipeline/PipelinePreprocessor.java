package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SentencePreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleArtifactPreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.codegraph.ComponentCreator;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation.CodeObjectsWriter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation.ComponentNamesWriter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation.ProjectNameWriter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation.SectionsSplitter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json.JsonConverterText;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json.JsonMergerArray;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json.JsonSplitterArray;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json.JsonSplitterMap;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.TemplateRequest;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.TextSplitterListing;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.LineIdentifier;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.RegexReplacer;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.StringJoiner;

import static edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier.CONFIG_NAME_SEPARATOR;

/**
 * A preprocessor that allows processing of artifacts with multiple preprocessors sequentially.
 * It is designed to act as a backbone for the overall artifact preprocessing, hence it is not present in the hierarchy itself.
 * Processing flow:
 * <ol>
 *     <li>An artifact preprocessor converts the input artifacts into an initial list of elements.</li>
 *     <li>Each pipelined preprocessor receives only those elements from the previous stage whose {@link Element#isCompare()} flag is {@code true}.</li>
 *     <li>Before forwarding these elements to the next stage, their {@code compare} flag is cleared (except before the final stage).
 * This prevents elements of intermediate stages to be included for classification.</li>
 *     <li>The outputs of every stage are uniquely accumulated and returned as a single list (initial artifact-preprocessor elements plus all elements produced by each stage).</li>
 * </ol>
 * Configuration:
 * <ul>
 *     <li>If the first configuration entry is not an artifact preprocessor, a {@link SingleArtifactPreprocessor} is inferred at the beginning.</li>
 *     <li>All following configuration entries are used to create element preprocessors in order.</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class PipelinePreprocessor extends Preprocessor {
    
    private final Preprocessor artifactPreprocessor;
    private final List<Pipelineable> stages;
    
    public PipelinePreprocessor(List<ModuleConfiguration> configurations, ContextStore contextStore) {
        super(contextStore);
        if (configurations.isEmpty()) {
            // TODO decide whether to actually enforce defining a preprocessor, as it would be defaulted anyway
            throw new IllegalArgumentException("at least one preprocessor must be defined");
        }
        Queue<ModuleConfiguration> queuedConfigurations = new LinkedList<>(configurations);
        this.artifactPreprocessor = retrieveArtifactPreprocessor(queuedConfigurations, contextStore);
        this.stages = retrievePreprocessors(queuedConfigurations, contextStore);
    }

    @Override
    public final List<Element> preprocess(List<Artifact> artifacts) {
        Collection<Element> elements = new LinkedHashSet<>(this.artifactPreprocessor.preprocess(artifacts));
        if (stages.isEmpty()) {
            return new ArrayList<>(elements);
        }
        
        for (Element element : elements) {
            element.setCompare(false);
        }
        
        List<Element> toCompare = new ArrayList<>(elements);
        List<Pipelineable> pipelinedPreprocessors = this.stages;
        for (int i = 0; i < pipelinedPreprocessors.size(); i++) {
            if (toCompare.isEmpty()) {
                logger.warn("stage {} returned no elements to compare, aborting pipeline", i);
                break;
            }
            Pipelineable preprocessor = pipelinedPreprocessors.get(i);
            List<Element> newElements = preprocessor.process(toCompare);
            
            // only elements designed to be classified are provided to the next preprocessor
            toCompare = newElements.stream().filter(Element::isCompare).toList();
            if (i < pipelinedPreprocessors.size() - 1) {
                for (Element element : toCompare) {
                    element.setCompare(false);
                }
            }
            elements.addAll(newElements);
        }
        return new ArrayList<>(elements);
    }

    private static Preprocessor retrieveArtifactPreprocessor(Queue<ModuleConfiguration> configurations, ContextStore contextStore) {
        if (!configurations.isEmpty()) {
            try {
                var preprocessor = Preprocessor.createPreprocessor(configurations.peek(), contextStore);
                configurations.poll(); // consume successfully created preprocessor
                return preprocessor;
            } catch (IllegalStateException ignored) {
                // gracefully exit exception to infer default artifact to element preprocessor
            }
        }
        return new SingleArtifactPreprocessor(contextStore);
    }
    
    private static List<Pipelineable> retrievePreprocessors(Collection<ModuleConfiguration> configurations, ContextStore contextStore) {
        List<Pipelineable> preprocessors = new ArrayList<>(configurations.size());
        for (ModuleConfiguration preprocessorConfiguration : configurations) {
            preprocessors.add(createStage(preprocessorConfiguration, contextStore));
        }
        return preprocessors;
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
    private static Pipelineable createStage(ModuleConfiguration configuration, ContextStore contextStore) {
        return switch (configuration.name().split(CONFIG_NAME_SEPARATOR)[0]) {
            case "documentationProjectName" -> new ProjectNameWriter(configuration, contextStore);
            case "documentationComponentNames" -> new ComponentNamesWriter(configuration, contextStore);
            case "documentationCodeObjects" -> new CodeObjectsWriter(configuration, contextStore);
            case "documentationSectionSplitter" -> new SectionsSplitter(configuration, contextStore);
            case "code" -> switch (configuration.name()) {
                case "code_component_creator" -> new ComponentCreator(contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "regex" -> switch (configuration.name()) {
                case "regex_replacer" -> new RegexReplacer(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "element" -> switch (configuration.name()) {
                case "element_joiner" -> new ElementJoiner(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "json" -> switch (configuration.name()) {
                case "json_splitter_array" -> new JsonSplitterArray(configuration, contextStore);
                case "json_converter_text" -> new JsonConverterText(configuration, contextStore);
                case "json_merger_array" -> new JsonMergerArray(configuration, contextStore);
                case "json_splitter_map" -> new JsonSplitterMap(contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "text" -> switch (configuration.name()) {
                case "text_splitter_listing" -> new TextSplitterListing(configuration, contextStore);
                case "text_line_id" -> new LineIdentifier(contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "template" -> switch (configuration.name()) {
                case "template_replace" -> new TemplateElement(configuration, contextStore);
                case "template_openai" -> new TemplateRequest(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "context" -> switch (configuration.name()) {
                case "context_writer" -> new ContextWriter(configuration, contextStore);
                default -> throw new IllegalArgumentException("Unsupported pipeline stage name: " + configuration.name());
            };
            case "sentence" -> new SentencePreprocessor(configuration, contextStore);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }
    
    public static String getLineIdPrefixedDocumentation(ContextStore contextStore) {
        String[] lines = contextStore.getContext("documentation", StringContext.class).asString().split("\n");
        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < lines.length; i++) {
            joiner.add((i + 1) + ": " + lines[i]);
        }
        return joiner.toString();
    }
}
