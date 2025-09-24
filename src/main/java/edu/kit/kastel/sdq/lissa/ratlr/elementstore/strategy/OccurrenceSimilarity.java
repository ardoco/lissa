package edu.kit.kastel.sdq.lissa.ratlr.elementstore.strategy;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class OccurrenceSimilarity extends MaxResultsStrategy {
    
    public OccurrenceSimilarity(ModuleConfiguration configuration) {
        super(configuration);
    }

    @Override
    public List<Pair<Element, Float>> findSimilarElementsInternal(Pair<Element, float[]> query, List<Pair<Element, float[]>> allElementsInStore) {
        List<Pair<Element, Float>> results = new ArrayList<>(allElementsInStore.size());
        String[] words = query.first().getContent().split("\\W+");
        for (Pair<Element, float[]> target : allElementsInStore) {
            String content = target.first().getContent().toLowerCase();
            int occurrences = 0;
            for (String word : words) {
                if (content.contains(word.toLowerCase())) {
                    occurrences++;
                }
            }
            results.add(new Pair<>(target.first(), (float) occurrences));
        }
        return results;
    }
}
