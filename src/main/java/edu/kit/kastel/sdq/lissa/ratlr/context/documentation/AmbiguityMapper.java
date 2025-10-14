package edu.kit.kastel.sdq.lissa.ratlr.context.documentation;

import edu.kit.kastel.sdq.lissa.ratlr.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AmbiguityMapper implements Context {

    private final String identifier;
    private final Map<SortedSet<String>, String> ambiguityInformation;

    public AmbiguityMapper(String identifier, Map<SortedSet<String>, String> ambiguityInformation) {
        this.identifier = identifier;
        this.ambiguityInformation = ambiguityInformation;
    }
    
    public boolean isAmbiguous(String value) {
        for (SortedSet<String> sharingAmbiguity : ambiguityInformation.keySet()) {
            if (sharingAmbiguity.contains(value)) {
                return true;
            }
        }
        return false;
    }
    
    public Map<SortedSet<String>, String> getSharedAmbiguities(String value) {
        Map<SortedSet<String>, String> sharedAmbiguities = new HashMap<>();
        for (Map.Entry<SortedSet<String>, String> sharingAmbiguity : ambiguityInformation.entrySet()) {
            if (sharingAmbiguity.getKey().contains(value)) {
                sharedAmbiguities.put(sharingAmbiguity.getKey().stream()
                        .filter(Predicate.not(value::equals))
                        .collect(Collectors.toCollection(TreeSet::new))
                    , sharingAmbiguity.getValue());
            }
        }
        return sharedAmbiguities;
    }

    @Override
    public String getId() {
        return identifier;
    }
}
