/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PromptOptimizationUtilsTest {

    static Stream<Arguments> sanitizePromptProvider() {
        return Stream.of(
                Arguments.of("hello", "hello"),
                Arguments.of("  hello  ", "hello"),
                Arguments.of("\"hello\"", "hello"),
                Arguments.of("'hello'", "hello"),
                Arguments.of("  'hello'  ", "hello"),
                Arguments.of("  \"hello\"  ", "hello"),
                Arguments.of("'''hello'''", "hello"),
                Arguments.of("\"'hello'\"", "hello"),
                Arguments.of("'\"hello\"'", "hello"),
                Arguments.of("  '\" hello \"'  ", "hello"),
                Arguments.of("", ""));
    }

    /**
     * Tests the prompt sanitization method with various inputs.
     *
     * @param input  the input prompt string
     * @param output the expected sanitized prompt string
     */
    @ParameterizedTest
    @MethodSource("sanitizePromptProvider")
    void testPromptSanitization(String input, String output) {
        String sanitizedPrompt = PromptOptimizationUtils.sanitizePrompt(input);
        Assertions.assertEquals(output, sanitizedPrompt, "Prompt sanitization failed.");
    }
}
