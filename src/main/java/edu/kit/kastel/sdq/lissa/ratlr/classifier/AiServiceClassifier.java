package edu.kit.kastel.sdq.lissa.ratlr.classifier;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.Optional;

public class AiServiceClassifier extends Classifier {
    
    
    
    /**
     * Creates a new classifier with the specified number of threads and context store.
     *
     * @param threads      The number of threads to use for parallel processing
     * @param contextStore The shared context store for pipeline components
     */
    protected AiServiceClassifier(ModuleConfiguration configuration, int threads, ContextStore contextStore) {
        super(threads, contextStore);
    }

    @Override
    protected Optional<ClassificationResult> classify(Element source, Element target) {
        return Optional.empty();
    }

    @Override
    protected Classifier copyOf() {
        return null;
    }
}
