package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

import java.util.LinkedList;
import java.util.List;

public class IndependentParallel extends PipelineStage {

    private final List<PipelineStage> preprocessors;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected IndependentParallel(ContextStore contextStore) {
        super(contextStore);
        this.preprocessors = List.of();
    }

    @Override
    public List<Element> process(List<Element> elements) {
        List<Element> results = new LinkedList<>();
        for (PipelineStage parallelStage : preprocessors) {
            results.addAll(parallelStage.process(elements));
        }
        return results;
    }
}
