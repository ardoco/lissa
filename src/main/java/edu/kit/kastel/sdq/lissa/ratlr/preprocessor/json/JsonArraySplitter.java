package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// TODO check necessity to extend from JsonPreprocessor
public class JsonArraySplitter extends JsonPreprocessor {

    private final Map<String, String> remapper;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    public JsonArraySplitter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(contextStore);
        this.remapper = configuration.argumentAsType("remapper", new TypeReference<>() {});
    }

    protected void preprocess(Element element, List<Element> elements) throws JsonProcessingException {
        List<Map<String, String>> results = splitOnEachArrayEntry(element.getContent());
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> result = results.get(i);
            elements.add(Element.fromParent(element, i, MAPPER.writeValueAsString(result), true));
        }
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
     * @throws JsonProcessingException if the input is not a valid JSON
     */
    @NotNull
    private List<Map<String, String>> splitOnEachArrayEntry(String json) throws JsonProcessingException {
        Map<String, JsonNode> children = MAPPER.readValue(json, new TypeReference<>() {});

        List<Map<String, String>> results = new LinkedList<>();
        results.add(new LinkedHashMap<>());
        for (Map.Entry<String, JsonNode> childEntry : children.entrySet()) {
            if (remapper.containsKey(childEntry.getKey()) && childEntry.getValue().isArray()) {
                if (childEntry.getValue().isEmpty()) {
                    continue;
                }

                // create a copy of every existing result for each array entry and append the entry on each
                List<Map<String, String>> newResults = new LinkedList<>();
                for (JsonNode arrayElement : childEntry.getValue()) {
                    for (Map<String, String> resultWithoutArrayElement : results) {
                        Map<String, String> newResult = new LinkedHashMap<>(resultWithoutArrayElement);
                        newResult.put(remapper.get(childEntry.getKey()), MAPPER.writeValueAsString(arrayElement));
                        newResults.add(newResult);
                    }
                }
                results = newResults;
            } else {
                // append non-splittable entry on every existing result
                for (Map<String, String> result : results) {
                    result.put(childEntry.getKey(), MAPPER.writeValueAsString(childEntry.getValue()));
                }
            }
        }
        return results;
    }
}
