package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.context.StringContext;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.pipeline.text.TemplateElement;
import edu.kit.kastel.sdq.lissa.ratlr.utils.formatter.ElementReplacementRetriever;
import edu.kit.kastel.sdq.lissa.ratlr.utils.formatter.TemplateFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
public class ContextWriter extends TemplateElement {
    
    private final Map<String, String> contextIdentifiersToStore = new HashMap<>();
    private final AtomicReference<Element> elementReference = new AtomicReference<>();
    private final TemplateFormatter formatter;
    
    /**
     * Creates a new preprocessor with the specified context store.
     *
     * @param contextStore The shared context store for pipeline components
     */
    protected ContextWriter(ModuleConfiguration configuration, ContextStore contextStore) {
        super(configuration, contextStore);
        this.formatter = new TemplateFormatter(configuration, new ElementReplacementRetriever(null, elementReference));
        for (String argumentKey : configuration.argumentKeys()) {
            if (!configuration.retrievedArgumentKeys().contains(argumentKey)) {
                this.contextIdentifiersToStore.put(argumentKey, configuration.argumentAsString(argumentKey));
            }
        }
    }

    @Override
    public List<Element> process(Element element) {
        elementReference.set(element);
        for (Map.Entry<String, String> toStoreEntry : this.contextIdentifiersToStore.entrySet()) {
            contextStore.createContext(new StringContext(toStoreEntry.getKey(), formatter.format()));
        }
        
        element.setCompare(true);
        return List.of(element);
    }
}
