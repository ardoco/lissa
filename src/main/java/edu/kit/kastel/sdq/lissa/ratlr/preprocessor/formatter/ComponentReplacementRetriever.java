package edu.kit.kastel.sdq.lissa.ratlr.preprocessor.formatter;

import edu.kit.kastel.sdq.lissa.ratlr.context.codegraph.component.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ComponentReplacementRetriever extends SupplyingRetriever<Component> {

    /**
     * The keying of component information.
     */
    private static final Map<String, Function<Component, String>> COMPONENT_VALUE_PROVIDER_BY_REPLACE_KEY = new HashMap<>() {{
        put("component_simple_name", Component::getSimpleName);
        put("component_qualified_name", Component::getQualifiedName);
        put("component_package_paths", component -> formatPaths(component.getPaths()));
    }};

    public ComponentReplacementRetriever(ReplacementRetriever retriever, AtomicReference<Component> valueReference) {
        super(retriever, valueReference);
    }

    @Override
    protected String retrieveReplacement(Component value, String placeholderKey) {
        return COMPONENT_VALUE_PROVIDER_BY_REPLACE_KEY.containsKey(placeholderKey)
                ? COMPONENT_VALUE_PROVIDER_BY_REPLACE_KEY.get(placeholderKey).apply(value)
                : null;
    }
    
    private static String formatPaths(Collection<String> paths) {
        StringJoiner joiner = new StringJoiner("\n");
        for (String packagePath : paths) {
            joiner.add(packagePath);
        }
        return joiner.toString();
    }
}
