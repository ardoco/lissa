package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public abstract class PipelineStage implements Pipelineable {
    
    /** Logger instance for this pipeline stage */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final ContextStore contextStore;

    protected PipelineStage(ContextStore contextStore) {
        this.contextStore = Objects.requireNonNull(contextStore);
    }
}
