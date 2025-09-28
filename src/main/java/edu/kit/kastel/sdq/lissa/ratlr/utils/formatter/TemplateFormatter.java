package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

import java.util.function.Function;

public class TemplateFormatter implements Formatter {

    private final ValueFormatter<String> textFormatter;
    
    public TemplateFormatter(ModuleConfiguration configuration, ReplacementRetriever retriever, String templateKey) {
        this.textFormatter = new TextFormatter(configuration, retriever);
        this.textFormatter.setValue(configuration.argumentAsString(templateKey));
    }

    public TemplateFormatter(ModuleConfiguration configuration, ReplacementRetriever retriever) {
        this(configuration, retriever, "template");
    }

    @Override
    public String format() {
        return textFormatter.format();
    }

    @Override
    public void addRetriever(Function<ReplacementRetriever, ReplacementRetriever> retrieverProvider) {
        textFormatter.addRetriever(retrieverProvider);
    }
}
