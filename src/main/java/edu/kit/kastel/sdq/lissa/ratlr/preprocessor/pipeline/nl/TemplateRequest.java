package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;

import java.util.ArrayList;
import java.util.List;

public class TemplateRequest extends LanguageModelRequester {
    
    // TODO consider decorator pattern instead
    protected final TemplateElement replacer;
    
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
    protected List<String> createRequests(List<Element> elements) {
        return this.replacer.process(elements).stream()
                .map(Element::getContent)
                .toList();
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        List<Element> result = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            result.add(Element.fromParent(element, responses.get(i)));
        }
        return result;
    }
}
