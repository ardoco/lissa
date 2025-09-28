package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;

import java.util.List;

public class RegexReplacer extends SingleElementProcessingStage {
    
    private final String regex;
    private final String replacement;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public RegexReplacer(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.regex = configuration.argumentAsString("regex");
        this.replacement = configuration.argumentAsString("replacement");
    }

    @Override
    protected List<Element> process(Element element) {
        return List.of(Element.fromParent(element, element.getContent().replaceAll(regex, replacement)));
    }
}
