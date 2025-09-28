package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ContextReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.ElementFormatter;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter.TemplateFormatter;

import java.util.List;
import java.util.StringJoiner;

public class ElementJoiner extends PipelineStage {
    
    private final TemplateFormatter templatePrefix;
    private final ElementFormatter templateElement;
    private final TemplateFormatter templateDelimiter;
    private final TemplateFormatter templateSuffix;
    private final String elementType;
    private int counter;
    
    protected ElementJoiner(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.templatePrefix = new TemplateFormatter(configuration, new ContextReplacementRetriever(null, contextStore), "prefix");
        this.templateElement = new ElementFormatter(configuration, contextStore);
        this.templateDelimiter = new TemplateFormatter(configuration, new ContextReplacementRetriever(null, contextStore), "delimiter");
        this.templateSuffix = new TemplateFormatter(configuration, new ContextReplacementRetriever(null, contextStore), "suffix");
        this.elementType = configuration.argumentAsString("type");
    }

    @Override
    public List<Element> process(List<Element> elements) {
        StringJoiner joiner = new StringJoiner(templateDelimiter.format(), templatePrefix.format(), templateSuffix.format());
        for (Element element : elements) {
            templateElement.setValue(element);
            joiner.add(templateElement.format());
        }
        return List.of(new Element("joined_element_" + counter++, elementType, joiner.toString(), elements.getFirst().getGranularity() + 1, null, true));
    }
}
