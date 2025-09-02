package edu.kit.kastel.sdq.lissa.ratlr.preprocessor;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.text.ElementFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A preprocessor that writes context into the context store.
 * It:
 * <ul>
 *     <li>Writes specifiable context for each element to the context store.</li>
 *     <li>Returns the provided element itself in a singleton list.</li>
 *     <li>Sets the {@link Element#isCompare() classification flag} of the provided element to true.</li>
 * </ul>
 * 
 * Configuration:
 * <ul>
 *     <li>{@code placeholder}: The string format of a placeholder, containing exactly one {@code %s} representing the identification key surrounded by arbitrary symbols</li>
 *     <li>
 *         Each remaining key is arbitrarily choosable and defines the identifier of the context to store.
 *         Each value thereby represents the context to be written. Their placeholders are resolved before storing.
 *     </li>
 * </ul>
 */
public class ContextWriter extends ElementFormatter {
    
    private final Map<String, String> contextIdentifiersToStore = new HashMap<>();
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected ContextWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        for (String argumentKey : configuration.argumentKeys()) {
            if (!configuration.retrievedArgumentKeys().contains(argumentKey)) {
                this.contextIdentifiersToStore.put(argumentKey, configuration.argumentAsString(argumentKey));
            }
        }
    }

    @Override
    protected List<Element> preprocess(Element element) {
        for (Map.Entry<String, String> toStoreEntry : this.contextIdentifiersToStore.entrySet()) {
            contextStore.createContext(new StringContext(toStoreEntry.getKey(), replace(toStoreEntry.getValue(), element)));
        }
        
        element.setCompare(true);
        return List.of(element);
    }
}
