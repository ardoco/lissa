package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json;

import com.fasterxml.jackson.core.type.TypeReference;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class JsonSplitterMap extends SingleElementProcessingStage {
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonSplitterMap(ContextStore contextStore) {
        super(contextStore);
    }

    @Override
    protected List<Element> process(Element element) {
        Map<String, List<String>> codeObjectInformation = Jsons.readValue(element.getContent(), new TypeReference<>() {});
        int id = 0;
        List<Element> results = new ArrayList<>(codeObjectInformation.size());
        for (Map.Entry<String, List<String>> entry : codeObjectInformation.entrySet()) {
            results.add(Element.fromParent(element, id, entry.getKey(), true));
            id++;
        }
        return results;
    }
}
