/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A utility class for generating deterministic unique keys from input strings.
 * This class provides functionality to:
 * <ul>
 *     <li>Generate deterministic UUIDs based on input strings</li>
 *     <li>Normalize line endings in input strings</li>
 *     <li>Create consistent keys for caching and identification purposes</li>
 * </ul>
 * <p>
 * The generated keys are:
 * <ul>
 *     <li>Deterministic - same input always produces the same key</li>
 *     <li>Unique - different inputs produce different keys</li>
 *     <li>Normalized - line endings are standardized</li>
 * </ul>
 */
public final class KeyGenerator {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private KeyGenerator() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Generates a deterministic UUID key from the given input string.
     * This method:
     * <ol>
     *     <li>Normalizes line endings in the input string</li>
     *     <li>Converts the string into a UUID format</li>
     * </ol>
     *
     * @param input The input string to generate a key from
     * @return A deterministic UUID based on the input string
     * @throws IllegalArgumentException if the input string is null
     */
    public static String generateKey(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        // Normalize lineendings
        String normalized = input.replace("\r\n", "\n");
        return UUID.nameUUIDFromBytes(normalized.getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}
