package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text;

import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;

import java.util.List;
import java.util.StringJoiner;

public class LineIdentifier extends SingleElementProcessingStage {
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public LineIdentifier(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    protected List<Element> process(Element element) {
        String[] lines = element.getContent().split("\n");
        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < lines.length; i++) {
            joiner.add((i + 1) + ": " + lines[i]);
        }
        return List.of(Element.fromParent(element, joiner.toString()));
    }
}
