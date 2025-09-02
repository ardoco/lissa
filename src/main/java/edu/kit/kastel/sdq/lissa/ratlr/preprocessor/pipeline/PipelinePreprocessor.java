package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Artifact;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.SingleArtifactPreprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
public class PipelinePreprocessor extends Preprocessor<Artifact> {
    
    private final Preprocessor<Artifact> artifactPreprocessor;
    private final List<Preprocessor<Element>> preprocessors;
    
    public PipelinePreprocessor(List<ModuleConfiguration> configurations, ContextStore contextStore) {
        super(contextStore);
        if (configurations.isEmpty()) {
            // TODO decide whether to actually enforce defining a preprocessor, as it would be defaulted anyway
            throw new IllegalArgumentException("at least one preprocessor must be defined");
        }
        Queue<ModuleConfiguration> queuedConfigurations = new LinkedList<>(configurations);
        this.artifactPreprocessor = retrieveArtifactPreprocessor(queuedConfigurations, contextStore);
        this.preprocessors = retrievePreprocessors(queuedConfigurations, contextStore);
    }

    @Override
    public final List<Element> preprocess(List<Artifact> artifacts) {
        Collection<Element> elements = new LinkedHashSet<>(this.artifactPreprocessor.preprocess(artifacts));
        List<Element> toCompare = new ArrayList<>(elements);
        List<Preprocessor<Element>> pipelinedPreprocessors = this.preprocessors;
        for (int i = 0; i < pipelinedPreprocessors.size(); i++) {
            Preprocessor<Element> preprocessor = pipelinedPreprocessors.get(i);
            List<Element> newElements = preprocessor.preprocess(toCompare);
            
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

    private static Preprocessor<Artifact> retrieveArtifactPreprocessor(Queue<ModuleConfiguration> configurations, ContextStore contextStore) {
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
    
    private static List<Preprocessor<Element>> retrievePreprocessors(Collection<ModuleConfiguration> configurations, ContextStore contextStore) {
        List<Preprocessor<Element>> preprocessors = new ArrayList<>(configurations.size());
        for (ModuleConfiguration preprocessorConfiguration : configurations) {
            preprocessors.add(createElementPreprocessor(preprocessorConfiguration, contextStore));
        }
        return preprocessors;
    }
}
