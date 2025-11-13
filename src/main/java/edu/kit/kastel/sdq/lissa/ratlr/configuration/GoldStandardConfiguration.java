/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.configuration;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents the configuration for gold standard evaluation in trace link analysis.
 * This record contains settings for loading and parsing gold standard data from a file.
 * The gold standard file is expected to be a CSV file containing trace links between
 * source and target elements.
 */
public record GoldStandardConfiguration(
        /**
         * Path to the gold standard file.
         * The file should be a CSV file containing trace links.
         */
        @JsonProperty("path") String path,

        /**
         * Whether the gold standard file has a header row.
         * If true, the first row will be skipped during parsing.
         */
        @JsonProperty(defaultValue = "false") boolean hasHeader,

        /**
         * Whether to swap the source and target columns when reading the file.
         * This is useful when the gold standard file has columns in a different order
         * than expected by the system.
         */
        @JsonProperty(value = "swap_columns", defaultValue = "false") boolean swapColumns) {

    /**
     * Loads a gold standard configuration from a JSON file.
     * If the file cannot be loaded or is null, returns null.
     *
     * @param evaluationConfig Path to the JSON configuration file
     * @return A new GoldStandardConfiguration instance, or null if loading fails
     */
    public static GoldStandardConfiguration load(Path evaluationConfig) {
        if (evaluationConfig == null) return null;

        try {
            return new ObjectMapper().readValue(evaluationConfig.toFile(), GoldStandardConfiguration.class);
        } catch (IOException e) {
            LoggerFactory.getLogger(GoldStandardConfiguration.class)
                    .error("Loading evaluation config threw an exception: {}", e.getMessage());
            return null;
        }
    }
}
