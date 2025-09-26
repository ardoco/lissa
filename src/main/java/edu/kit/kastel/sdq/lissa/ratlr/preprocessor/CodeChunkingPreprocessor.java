/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * A preprocessor that splits source code artifacts into chunks of a specified size.
 * This preprocessor creates a hierarchical structure of elements where:
 * <ul>
 *     <li>The root element represents the entire code file (granularity level 0)</li>
 *     <li>Child elements represent code chunks (granularity level 1)</li>
 * </ul>
 *
 * The preprocessor supports multiple programming languages and can automatically
 * detect the language based on the file extension. Each code chunk is created
 * using language-specific splitting rules to maintain code structure and readability.
 *
 * Configuration options:
 * <ul>
 *     <li>language: The programming language(s) to use. Can be multiple languages
 *         separated by commas. If multiple languages are specified, the language
 *         is determined by the file extension of each artifact.</li>
 *     <li>chunk_size: The target size for each code chunk (default: 60)</li>
 * </ul>
 *
 * Each code chunk element:
 * <ul>
 *     <li>Has a unique identifier combining the artifact ID and chunk index</li>
 *     <li>Maintains the same type as the source artifact</li>
 *     <li>Contains a portion of the source code as its content</li>
 *     <li>Has granularity level 1</li>
 *     <li>Is marked for comparison (compare=true)</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class CodeChunkingPreprocessor extends Preprocessor {

    /** The list of supported programming languages */
    private final List<RecursiveSplitter.Language> languages;
    /** The target size for each code chunk */
    private final int chunkSize;

    /**
     * Creates a new code chunking preprocessor with the specified configuration.
     *
     * @param configuration The module configuration containing language and chunk size settings
     * @param contextStore The shared context store for pipeline components
     */
    public CodeChunkingPreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.languages = Arrays.stream(
                        configuration.argumentAsString("language").split(","))
                .map(RecursiveSplitter.Language::valueOf)
                .toList();
        this.chunkSize = configuration.argumentAsInt("chunk_size", 60);
    }

    /**
     * Preprocesses a list of code artifacts by splitting each one into chunks.
     * For each artifact, this method:
     * <ol>
     *     <li>Creates an element representing the entire code file</li>
     *     <li>Splits the code into chunks based on the configured language and chunk size</li>
     *     <li>Creates elements for each code chunk</li>
     *     <li>Links chunk elements to the file element</li>
     * </ol>
     *
     * @param artifacts The list of code artifacts to preprocess
     * @return A list of elements containing both the original code files and their chunks
     */
    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        List<Element> elements = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            List<Element> preprocessed = preprocess(artifact);
            elements.addAll(preprocessed);
        }
        return elements;
    }

    /**
     * Preprocesses a single code artifact by splitting it into chunks.
     * This method:
     * <ol>
     *     <li>Creates an element for the entire code file (granularity level 0)</li>
     *     <li>Generates code chunks using language-specific splitting rules</li>
     *     <li>Creates elements for each code chunk (granularity level 1)</li>
     *     <li>Links chunk elements to the file element</li>
     * </ol>
     *
     * @param artifact The code artifact to preprocess
     * @return A list of elements containing both the original code file and its chunks
     */
    protected List<Element> preprocess(Artifact artifact) {
        List<String> segments = this.generateSegments(artifact);
        List<Element> elements = new ArrayList<>();

        Element artifactAsElement =
                new Element(artifact.getIdentifier(), artifact.getType(), artifact.getContent(), 0, null, false);
        elements.add(artifactAsElement);

        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            Element segmentAsElement = new Element(
                    artifact.getIdentifier() + SEPARATOR + i, artifact.getType(), segment, 1, artifactAsElement, true);
            elements.add(segmentAsElement);
        }

        return elements;
    }

    /**
     * Generates code chunks from an artifact using language-specific splitting rules.
     * The language is determined either from the configuration (if only one language
     * is specified) or from the file extension of the artifact.
     *
     * @param artifact The code artifact to split into chunks
     * @return A list of code chunks
     */
    private List<String> generateSegments(Artifact artifact) {
        RecursiveSplitter.Language language = languages.size() == 1 ? languages.getFirst() : getLanguage(artifact);
        return RecursiveSplitter.fromLanguage(language, chunkSize).splitText(artifact.getContent());
    }

    /**
     * Determines the programming language of an artifact based on its file extension.
     * Currently supports:
     * <ul>
     *     <li>.java files -> JAVA</li>
     *     <li>.py files -> PYTHON</li>
     * </ul>
     *
     * @param artifact The code artifact to determine the language for
     * @return The programming language of the artifact
     * @throws IllegalArgumentException if the file extension is not supported
     */
    private RecursiveSplitter.Language getLanguage(Artifact artifact) {
        String ending =
                artifact.getIdentifier().substring(artifact.getIdentifier().lastIndexOf(".") + 1);
        return switch (ending) {
            case "java" -> RecursiveSplitter.Language.JAVA;
            case "py" -> RecursiveSplitter.Language.PYTHON;
            default -> throw new IllegalArgumentException("Unsupported language: " + ending);
        };
    }
}
