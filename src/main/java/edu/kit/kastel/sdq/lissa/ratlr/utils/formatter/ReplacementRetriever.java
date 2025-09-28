package edu.kit.kastel.sdq.lissa.ratlr.utils.formatter;

public interface ReplacementRetriever {
    
    /**
     * Retrieves the string that replaces the placeholder.
     *
     * @param placeholderKey the key identifying the placeholder
     * @return the string that replaces the placeholder, {@code null} if no replacement for this key can be found
     */
    String retrieveReplacement(String placeholderKey);
}
