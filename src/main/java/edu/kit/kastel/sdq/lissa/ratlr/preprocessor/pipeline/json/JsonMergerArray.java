package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json;

import com.fasterxml.jackson.databind.JsonNode;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.PipelineStage;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JsonMergerArray extends PipelineStage {

    
    private final Map<String, String> remapper;
    
    public JsonMergerArray(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.remapper = new HashMap<>();
        for (String key : configuration.argumentKeys()) {
            this.remapper.put(key, configuration.argumentAsString(key));
        }
    }

    @Override
    public List<Element> process(List<Element> elements) {
        Map<String, List<String>> codeObjectInformation = new TreeMap<>();
        for (Element element : elements) {
            JsonNode rootObject = Jsons.readTree(element.getContent());
            for (Map.Entry<String, String> entry : remapper.entrySet()) {
                String name = rootObject.get(entry.getKey()).textValue();
                codeObjectInformation.putIfAbsent(name, new LinkedList<>());
                codeObjectInformation.get(name).add(rootObject.get(entry.getValue()).textValue());
            }
        }
        return List.of(new Element("json_merge$0", "software object information", Jsons.writeValueAsString(codeObjectInformation), 0, null, true));
    }
}
