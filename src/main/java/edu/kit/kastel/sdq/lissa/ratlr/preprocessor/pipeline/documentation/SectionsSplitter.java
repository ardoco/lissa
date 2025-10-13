package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.documentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.PipelinePreprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl.LanguageModelRequester;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.IntStream;

public class SectionsSplitter extends LanguageModelRequester {

    private static final Logger LOGGER = LoggerFactory.getLogger(SectionsSplitter.class);
    private static final String SYSTEM_MESSAGE_TEMPLATE_DEFAULT =
            "Your task is to divide the provided documentation, that describes the software architecture of a software project, into sections. Each sentence of the documentation is prefixed with its identifier. The documentation has been slightly adapted to contain only sentences; images and tables were removed. Assume that the **whole** original documentation were divided into sections describing the high-level overview of the architecture, followed by sections describing each component more in-depth. Divide the whole documentation into these sections and extract them. Return for each section its title, the corresponding sentences with their identifier and a summary of what the section describes or its purpose in that documentation.";
    private static final String USER_MESSAGE_FORMAT = "%s";
    private static final String JSON_SCHEMA_TEMPLATE_DEFAULT =
            "{\"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\"title\": \"SectionExtraction\",\"description\": \"Extraction of sections of a software architecture documentation.\",\"type\": \"object\",\"properties\": {\"sections\": {\"description\": \"The extracted sections of the software architecture documentation.\",\"type\": \"array\",\"items\": {\"type\": \"object\",\"properties\": {\"title\": {\"description\": \"The title of the section.\",\"type\": \"string\"},\"sentences\": {\"description\": \"The sentences that correspond to the section, including their identifier.\",\"type\": \"array\",\"items\": {\"type\": \"object\", \"properties\": {\"identifier\": {\"description\": \"The identifier for this sentence.\",\"type\": \"integer\"}, \"content\": {\"description\": \"The verbatim content of this sentence.\",\"type\": \"string\"}}, \"required\": [\"identifier\", \"content\"]}},\"summary\": {\"description\": \"A summary of what the sections describes or its purpose in the documentation.\",\"type\": \"string\"}},\"required\": [\"title\", \"sentences\", \"summary\"]}, \"minItems\":<<<context_component_names_count>>>}},\"required\": [\"sections\"]}";

    public SectionsSplitter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore, SYSTEM_MESSAGE_TEMPLATE_DEFAULT, JSON_SCHEMA_TEMPLATE_DEFAULT);
    }

    @Override
    protected List<String> createRequests(List<Element> elements) {
        return List.of(USER_MESSAGE_FORMAT.formatted(PipelinePreprocessor.getLineIdPrefixedDocumentation(contextStore)));
    }

    @Override
    protected List<Element> createElements(List<Element> elements, List<String> responses) {
        SectionExtraction extraction = Jsons.readValue(responses.getFirst(), new TypeReference<>() {});
        int documentationLines = contextStore.getContext("documentation", StringContext.class).asString().split("\n").length;
        TreeSet<Integer> missedIdentifiers = IntStream
                .range(1, documentationLines + 1)
                .collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        List<Element> results = new ArrayList<>(documentationLines);
        for (Section section : extraction.sections) {
            for (Sentence sentence : section.sentences) {
                if (!missedIdentifiers.remove(sentence.identifier)) {
                    LOGGER.warn("identifier '{}' appeared again with sentence '{}' in section '{}'"
                            , sentence.identifier, sentence.content, section.title);
                }
                SectionSentence sectionSentence = new SectionSentence(sentence.identifier, sentence.content, section.title, section.summary);
                results.add(new Element("section_sentence_split" + Preprocessor.SEPARATOR + (sentence.identifier - 1), "software architecture documentation", 
                        Jsons.writeValueAsString(sectionSentence), 0, null, true));
            }
        }
        if (!missedIdentifiers.isEmpty()) {
            LOGGER.warn("missing identifiers: {}", missedIdentifiers);
        }
        
        return results;
    }
    
    private static final class SectionExtraction {
        @JsonProperty
        private List<Section> sections;
    }
    
    private static final class Section {
        @JsonProperty
        private String title;
        @JsonProperty
        private List<Sentence> sentences;
        @JsonProperty
        private String summary;
    }
    
    private static final class Sentence {
        @JsonProperty
        private int identifier;
        @JsonProperty
        private String content;
    }
    
    private static final class SectionSentence {
        @JsonProperty
        private int identifier;
        @JsonProperty
        private String content;
        @JsonProperty
        private String sectionTitle;
        @JsonProperty
        private String sectionSummary;
        public SectionSentence(int identifier, String content, String sectionTitle, String sectionSummary) {
            this.identifier = identifier;
            this.content = content;
            this.sectionTitle = sectionTitle;
            this.sectionSummary = sectionSummary;
        }
    }
}
