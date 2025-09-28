package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

public class TemplateFormatter {
    
    /**
     * The base template used for all values.
     */
    private final String template;
    private final TextReplacer replacer;
    
    public TemplateFormatter(ModuleConfiguration configuration, ReplacementRetriever retriever) {
        this.template = configuration.argumentAsString("template");
        this.replacer = new TextReplacer(configuration, retriever);
    }
    
    public String format() {
        return replacer.replace(template);
    }
}
