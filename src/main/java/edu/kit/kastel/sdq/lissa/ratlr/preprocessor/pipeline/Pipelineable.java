package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.util.List;

public interface Pipelineable {

    List<Element> process(List<Element> elements);
}
