package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.preprocessor.Preprocessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * An abstract preprocessor that offers to define variable placeholders and replacing them in texts.
 *
 * <p>
 *     Identifying placeholders is done by using a specifiable format.
 *     Each placeholder will be replaced by content using the contained key.
 *     The keys {@code element_content},
 *     {@code element_type},
 *     {@code element_identifier},
 *     {@code element_granularity},
 *     {@code element_parent},
 *     {@code element_parentId},
 *     {@code element_compare} return the corresponding serialized value of a provided element.
 *     A placeholder key must always be resolvable to yield a replacement when invoking {@link #retrieveReplacement(String)}.
 *     Subclasses overriding this method must obey this convention; however, it is not required, though recommended, to call the super method
 *     to retain resolving information of the element.
 * </p>
 *
 * <p>
 *     Configuration options:
 *     <ul>
 *         <li>{@code placeholder}: The string format of a placeholder, containing exactly one {@code %s} representing the identification key surrounded by arbitrary symbols</li>
 *     </ul>
 * </p>
 *
 * <p>Context handling is managed by the {@link Preprocessor} superclass. Subclasses should not duplicate context parameter documentation.</p>
 */
public class ElementReplacementRetriever extends SupplyingRetriever<Element> {

    /**
     * The keying of element information.
     */
    private static final Map<String, Function<Element, String>> ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY = new HashMap<>() {{
        put("element_content", Element::getContent);
        put("element_type", Element::getType);
        put("element_identifier", Element::getIdentifier);
        put("element_granularity", element -> String.valueOf(element.getGranularity()));
        put("element_parent", element -> String.valueOf(element.getParent()));
        put("element_parentId", element -> element.getParent().getIdentifier());
        put("element_compare", element -> String.valueOf(element.isCompare()));
    }};

    /**
     * Creates a new instance.
     * @param retriever the parent retriever, which is invoked first
     * @param valueReference the reference providing the value for {@link #retrieveReplacement(Element, String)}
     */
    public ElementReplacementRetriever(ReplacementRetriever retriever, AtomicReference<Element> valueReference) {
        super(retriever, valueReference);
    }

    /**
     * Retrieves the string that replaces the placeholder.
     *
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    @Override
    public String retrieveReplacement(Element element, String placeholderKey) {
        return ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY.containsKey(placeholderKey) 
                ? ELEMENT_VALUE_PROVIDER_BY_REPLACE_KEY.get(placeholderKey).apply(element) 
                : null;
    }
}
