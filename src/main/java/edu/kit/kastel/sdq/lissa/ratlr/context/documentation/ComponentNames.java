package edu.kit.kastel.sdq.lissa.ratlr.context.documentation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.lissa.ratlr.context.SerializedContext;

import java.util.Collections;
import java.util.SortedSet;
import java.util.stream.Collectors;

public class ComponentNames implements SerializedContext {
    
    public static final String IDENTIFIER = "documentation_component_names";
    @JsonProperty
    private final SortedSet<String> names;

    @JsonCreator
    public ComponentNames(@JsonProperty("names") SortedSet<String> names) {
        this.names = names;
    }

    public SortedSet<String> getNames() {
        return Collections.unmodifiableSortedSet(names);
    }
    
    public String asListing() {
        return names.stream().collect(Collectors.joining("\n- ", "- ", ""));
    }
    
    public String asJsonArrayWithoutBrackets() {
        return names.stream().collect(Collectors.joining("\", \"", "\"", "\""));
    }
    
    public int count() {
        return names.size();
    }

    @Override
    public String getId() {
        return IDENTIFIER;
    }

    @Override
    public String asString() {
        return asListing();
    }
}
