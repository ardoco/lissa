/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.kit.kastel.sdq.lissa.ratlr.Statistics;

/**
 * Test class for Statistics utility methods.
 */
class StatisticsTest {

    @ParameterizedTest
    @MethodSource("escapeMarkdownTestCases")
    void testEscapeMarkdown(String input, String expected) {
        assertEquals(expected, Statistics.escapeMarkdown(input));
    }

    static Stream<Arguments> escapeMarkdownTestCases() {
        return Stream.of(
                Arguments.of("Text with \\ backslash", "Text with \\\\ backslash"),
                Arguments.of("Code `example`", "Code \\`example\\`"),
                Arguments.of("Bold *text*", "Bold \\*text\\*"),
                Arguments.of("Italic _text_", "Italic \\_text\\_"),
                Arguments.of("Braces {test}", "Braces \\{test\\}"),
                Arguments.of("Link [text]", "Link \\[text\\]"),
                Arguments.of("Parentheses (text)", "Parentheses \\(text\\)"),
                Arguments.of("Header #1", "Header \\#1"),
                Arguments.of("Plus + sign", "Plus \\+ sign"),
                Arguments.of("Minus - sign", "Minus \\- sign"),
                Arguments.of("List item.", "List item\\."),
                Arguments.of("Image !", "Image \\!"),
                Arguments.of("Table | cell", "Table \\| cell"),
                Arguments.of("Quote > text", "Quote \\> text"),
                Arguments.of("*Bold* _italic_ `code`", "\\*Bold\\* \\_italic\\_ \\`code\\`"),
                Arguments.of("Plain text without special characters", "Plain text without special characters"),
                Arguments.of("", ""),
                Arguments.of("Text with\nnewline", "\n```\nText with\nnewline\n```"),
                Arguments.of("A".repeat(81), "\n```\n" + "A".repeat(81) + "\n```"),
                Arguments.of("A".repeat(80), "A".repeat(80)),
                Arguments.of("\\`*_{}[]()#+-.!|>", "\\\\\\`\\*\\_\\{\\}\\[\\]\\(\\)\\#\\+\\-\\.\\!\\|\\>"));
    }
}
