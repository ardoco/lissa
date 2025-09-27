package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.nl;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TextSplitterListing extends SingleElementProcessingStage {
    
    private final String listElementIndicator;
    private final String elementPrefix;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public TextSplitterListing(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.listElementIndicator = configuration.argumentAsString("listing_element_indicator", "- ");
        this.elementPrefix = configuration.argumentAsString("element_prefix", "");
    }

    @Override
    public List<Element> process(Element element) {
        String original = element.getContent();
        List<StringBuilder> results = new LinkedList<>();
        results.add(new StringBuilder());
        for (String normalTextOrList : original.splitWithDelimiters("(?m)((?<=^)%s.*\\n?)+".formatted(this.listElementIndicator), -1)) {
            if (normalTextOrList.startsWith(this.listElementIndicator)) {
                // list cluster found
                // create a copy of every existing result for each list entry and append the entry on each
                String[] listEntries = normalTextOrList.split("\n");
                List<StringBuilder> newResults = new ArrayList<>(results.size() * listEntries.length);
                for (String listEntry : listEntries) {
                    for (StringBuilder result : results) {
                        StringBuilder newResult = new StringBuilder(result);
                        newResult.append(this.elementPrefix).append(listEntry.substring(this.listElementIndicator.length()));
                        newResults.add(newResult);
                    }
                }
                results = newResults;
            } else {
                // treat as normal text
                // append non-splittable entry on every existing result
                for (StringBuilder result : results) {
                    result.append(normalTextOrList);
                }
            }
        }

        return getElements(element, results);
    }

    @NotNull
    private static List<Element> getElements(Element element, List<StringBuilder> results) {
        List<Element> elements = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            StringBuilder result = results.get(i);
            elements.add(Element.fromParent(element, i, result.toString(), true));
        }
        return elements;
    }
}
