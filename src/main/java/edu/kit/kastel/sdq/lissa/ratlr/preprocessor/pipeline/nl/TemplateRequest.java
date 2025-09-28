package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;

import java.util.List;

public class TemplateRequest extends LanguageModelPreprocessor {
    
    // TODO consider decorator pattern instead
    private final TemplateElement replacer;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param moduleConfiguration The module configuration containing template and model settings
     * @param contextStore        The shared context store for pipeline components
     */
    public TemplateRequest(ModuleConfiguration moduleConfiguration, ContextStore contextStore) {
        super(moduleConfiguration, contextStore);
        this.replacer = new TemplateElement(moduleConfiguration, contextStore);
    }

    @Override
    protected String createRequest(Element element) {
        return this.replacer.process(List.of(element)).getFirst().getContent();
    }

    @Override
    protected List<Element> createElements(Element element, String response) {
        return List.of(Element.fromParent(element, response));
    }
}
