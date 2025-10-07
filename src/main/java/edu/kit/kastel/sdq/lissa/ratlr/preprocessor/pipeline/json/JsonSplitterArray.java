package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.SingleElementProcessingStage;
import edu.kit.kastel.sdq.lissa.ratlr.utils.json.Jsons;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JsonSplitterArray extends SingleElementProcessingStage {

    private final String emptyDefault;
    private final Map<String, String> remapper;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonSplitterArray(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.emptyDefault = configuration.argumentAsString("empty_default", "");
        this.remapper = new HashMap<>();
        for (String key : configuration.argumentKeys()) {
            if (!key.equals("empty_default")) {
                this.remapper.put(key, configuration.argumentAsString(key));
            }
        }
    }

    protected List<Element> process(Element element) {
        List<String> splitResults = splitOnEachArrayEntry(element.getContent());
        List<Element> results = new LinkedList<>();
        for (int i = 0; i < splitResults.size(); i++) {
            results.add(Element.fromParent(element, i, splitResults.get(i), true));
        }
        return results;
    }

    /**
     * Splits a JSON by its array entries.
     * 
     * <p>Only arrays with a key that is registered as key in {@link #remapper} are considered for splitting.
     * Otherwise, it is ignored and treated as non-splittable like any other entry in the JSON.
     * Furthermore, only top level entries of the JSON are checked for splitting.</p> 
     * 
     * <p>The returned list will contain every combination of remapped array entries while keeping the original total order.
     * Therefore, the number of resulting combinations will be the length of every splittable array multiplied together.
     * Empty arrays, however, will be ignored and not contained in the results i.i.f. they were registered for {@link #remapper remapping}.</p>
     * 
     * <p>Example:
     * <pre>{@code 
     * remapper = {"foos":"foo","bars":"bar"}
     * json = {"f":"f","foos":["f_foo","s_foo"],"s":["f","s"],"bars":["f_bar","s_bar","t_bar"],"t":"t"}
     * 
     * result = [
     *   {"f":"f","foo":"f_foo","s":["f","s"],"bar":"f_bar","t":"t"},
     *   {"f":"f","foo":"s_foo","s":["f","s"],"bar":"f_bar","t":"t"},
     *   {"f":"f","foo":"f_foo","s":["f","s"],"bar":"s_bar","t":"t"},
     *   {"f":"f","foo":"s_foo","s":["f","s"],"bar":"s_bar","t":"t"},
     *   {"f":"f","foo":"f_foo","s":["f","s"],"bar":"t_bar","t":"t"},
     *   {"f":"f","foo":"s_foo","s":["f","s"],"bar":"t_bar","t":"t"}
     * ]}</pre></p>
     * @param json the input to process
     * @return a list containing combinations of split and remapped array entries along with the other content
     */
    @NotNull
    private List<String> splitOnEachArrayEntry(String json) {
//        Map<String, JsonNode> children = Jsons.readValue(json, new TypeReference<>() {});
//        while (children.size() == 1 && !remapper.containsKey(children.keySet().iterator().next()) && children.values().iterator().next().isObject()) {
//            children = Jsons.readValue(children.values().iterator().next().toString(), new TypeReference<>() {});
//        }

        JsonNode root = Jsons.readTree(json);
        while (root.size() == 1 && remapper.keySet().stream().noneMatch(root::has)) {
            root = root.iterator().next();
        }

        Map<String, JsonNode> children = new LinkedHashMap<>();
        Iterator<String> it = root.fieldNames();
        while (it.hasNext()) {
            String fieldName = it.next();
            children.put(fieldName, root.get(fieldName));
        }
        
        List<Map<String, JsonNode>> results = new LinkedList<>();
        results.add(new LinkedHashMap<>());
        for (Map.Entry<String, JsonNode> childEntry : children.entrySet()) {
            if (remapper.containsKey(childEntry.getKey()) && childEntry.getValue().isArray()) {
                // create a copy of every existing result for each array entry and append the entry on each
                List<Map<String, JsonNode>> newResults = new LinkedList<>();
                // TODO strip component suffix
                for (JsonNode arrayElement : (childEntry.getValue().isEmpty() ? new TextNode(emptyDefault) : childEntry.getValue())) {
                    for (Map<String, JsonNode> resultWithoutArrayElement : results) {
                        Map<String, JsonNode> newResult = new LinkedHashMap<>(resultWithoutArrayElement);
                        newResult.put(remapper.get(childEntry.getKey()), arrayElement);
                        newResults.add(newResult);
                    }
                }
                results = newResults;
            } else {
                // append non-splittable entry on every existing result
                for (Map<String, JsonNode> result : results) {
                    result.put(childEntry.getKey(), childEntry.getValue());
                }
            }
        }
        
        return results.stream().map(Jsons::writeValueAsString).toList();
    }
}
