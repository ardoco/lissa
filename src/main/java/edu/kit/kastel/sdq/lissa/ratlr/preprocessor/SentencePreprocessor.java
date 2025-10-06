/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;

/**
 * A preprocessor that splits text artifacts into sentences.
 * This preprocessor creates a hierarchical structure of elements where:
 * <ul>
 *     <li>The root element represents the entire artifact (granularity level 0)</li>
 *     <li>Child elements represent individual sentences (granularity level 1)</li>
 * </ul>
 *
 * The preprocessor uses the {@link DocumentBySentenceSplitter} to split the text
 * into sentences, creating a new element for each sentence. The original artifact
 * is also preserved as an element with granularity level 0, and all sentence elements
 * are linked to it as children.
 * <p>
 * Each sentence element:
 * <ul>
 *     <li>Has a unique identifier combining the artifact ID and sentence index</li>
 *     <li>Maintains the same type as the source artifact</li>
 *     <li>Contains only the sentence text as its content</li>
 *     <li>Has granularity level 1</li>
 *     <li>Is marked for comparison (compare=true)</li>
 * </ul>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class SentencePreprocessor extends Preprocessor {
    /**
     * Creates a new sentence preprocessor with the specified configuration.
     *
     * @param configuration The module configuration (currently unused)
     * @param contextStore The shared context store for pipeline components
     */
    public SentencePreprocessor(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
    }

    /**
     * Preprocesses a list of artifacts by splitting each one into sentences.
     * For each artifact, this method:
     * <ol>
     *     <li>Creates an element representing the entire artifact</li>
     *     <li>Splits the artifact's content into sentences</li>
     *     <li>Creates elements for each sentence</li>
     *     <li>Links sentence elements to the artifact element</li>
     * </ol>
     *
     * @param artifacts The list of artifacts to preprocess
     * @return A list of elements containing both the original artifacts and their sentences
     */
    @Override
    public List<Element> preprocess(List<Artifact> artifacts) {
        List<Element> elements = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            List<Element> preprocessed = preprocessIntern(artifact);
            elements.addAll(preprocessed);
        }
        return elements;
    }

    /**
     * Preprocesses a single artifact by splitting it into sentences.
     * This method:
     * <ol>
     *     <li>Creates an element for the entire artifact (granularity level 0)</li>
     *     <li>Uses {@link DocumentBySentenceSplitter} to split the content into sentences</li>
     *     <li>Creates elements for each sentence (granularity level 1)</li>
     *     <li>Links sentence elements to the artifact element</li>
     * </ol>
     *
     * @param artifact The artifact to preprocess
     * @return A list of elements containing both the original artifact and its sentences
     */
    private List<Element> preprocessIntern(Artifact artifact) {
        DocumentBySentenceSplitter splitter = new DocumentBySentenceSplitter(Integer.MAX_VALUE, 0);
        String[] sentences = splitter.split(artifact.getContent());
        List<Element> elements = new ArrayList<>();

        Element artifactAsElement =
                new Element(artifact.getIdentifier(), artifact.getType(), artifact.getContent(), 0, null, false);
        elements.add(artifactAsElement);

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            Element sentenceAsElement = new Element(
                    artifact.getIdentifier() + SEPARATOR + i, artifact.getType(), sentence, 1, artifactAsElement, true);
            elements.add(sentenceAsElement);
        }
        return elements;
    }
}
