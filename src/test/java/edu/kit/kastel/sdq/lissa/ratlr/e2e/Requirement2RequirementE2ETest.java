/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.e2e;

import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.escapeMarkdown;
import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.getTraceLinksFromGoldStandard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.kit.kastel.mcse.ardoco.metrics.ClassificationMetricsCalculator;
import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.Optimization;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;

class Requirement2RequirementE2ETest {

    @BeforeEach
    void setUp() throws IOException {
        File envFile = new File(".env");
        if (!envFile.exists() && System.getenv("CI") != null) {
            Files.writeString(envFile.toPath(), """
OLLAMA_EMBEDDING_HOST=http://localhost:11434
OLLAMA_HOST=http://localhost:11434
OPENAI_ORGANIZATION_ID=DUMMY
OPENAI_API_KEY=sk-DUMMY
""");
        }
    }

    @Test
    void testEnd2End() throws Exception {
        File config = new File("src/test/resources/warc/config.json");
        Assertions.assertTrue(config.exists());

        Evaluation evaluation = new Evaluation(config.toPath());
        var traceLinks = evaluation.run();

        Set<TraceLink> validTraceLinks =
                getTraceLinksFromGoldStandard(evaluation.getConfiguration().goldStandardConfiguration());

        ClassificationMetricsCalculator cmc = ClassificationMetricsCalculator.getInstance();
        var classification = cmc.calculateMetrics(traceLinks, validTraceLinks, null);
        classification.prettyPrint();
        Assertions.assertEquals(0.38, classification.getPrecision(), 1E-8);
        Assertions.assertEquals(0.6985294117647058, classification.getRecall(), 1E-8);
        Assertions.assertEquals(0.49222797927461137, classification.getF1(), 1E-8);
    }

    /**
     * Provides test cases for the parameterized end-to-end optimization tests.
     * Each test case consists of a configuration file path and the expected optimized prompt.
     * The optimized prompts can be copied from the output Markdown files.
     *
     * @return A stream of arguments for the parameterized tests.
     * @throws Exception If reading the expected prompt files fails.
     */
    private static Stream<Arguments> provideOptimizationTestCases() throws Exception {
        return Stream.of(
                Arguments.of(
                        "src/test/resources/warc/WARC_simple_gpt_gpt-4o-mini-2024-07-18_0.json",
                        Files.readString(Path.of("src/test/resources/expected/SimpleOptimizerExpectation.txt"))),
                Arguments.of(
                        "src/test/resources/warc/WARC_iterative_gpt_gpt-4o-mini-2024-07-18_0.json",
                        Files.readString(Path.of("src/test/resources/expected/IterativeOptimizerExpectation.txt"))),
                Arguments.of(
                        "src/test/resources/warc/WARC_feedback_gpt_gpt-4o-mini-2024-07-18_0_mi5_fs3.json",
                        Files.readString(Path.of("src/test/resources/expected/FeedbackOptimizerExpectation.txt"))),
                Arguments.of(
                        "src/test/resources/warc/WARC_gradient_gpt_gpt-4o-mini-2024-07-18_0.json",
                        Files.readString(Path.of("src/test/resources/expected/GradientOptimizerExpectation.txt"))));
    }

    /**
     * Tests the end-to-end functionality of the prompt optimizers using parameterized test cases.
     * Each test case is defined by a configuration file path and the expected optimized prompt.
     *
     * @param configPath   The path to the configuration file for the optimization run.
     * @param expectedPrompt The expected optimized prompt after running the optimization.
     */
    @ParameterizedTest
    @MethodSource("provideOptimizationTestCases")
    void testEnd2EndOptimizers(String configPath, String expectedPrompt) throws Exception {
        File config = new File(configPath);
        Assertions.assertTrue(
                config.exists(), "The configuration file should exist at %s".formatted(config.getAbsolutePath()));
        Optimization optimization = new Optimization(config.toPath());
        List<String> optimizedPrompts = optimization.run();
        Assertions.assertFalse(
                optimizedPrompts.isEmpty(),
                "The optimizer returned no prompts; as maximum_iterations > 0 and that the optimizer produces at least"
                        + " one prompt.");
        String optimizedPrompt = optimizedPrompts.getLast();
        String escapedOptimizedPrompt = escapeMarkdown(optimizedPrompt);

        Assertions.assertEquals(
                expectedPrompt.lines().map(String::strip).toList(),
                escapedOptimizedPrompt.lines().map(String::strip).toList(),
                "The optimized prompt does not match every line (regardless of line terminators or leading/trailing "
                        + "spaces) of the expected prompt.");
    }

    /**
     * Provides test cases for default configuration smoke tests.
     * These tests ensure that default configurations execute without throwing errors.
     * Expected prompts can be added later by creating corresponding expectation files.
     *
     * @return A stream of arguments for the parameterized tests.
     */
    private static Stream<Arguments> provideDefaultConfigTestCases() {
        return Stream.of(
                Arguments.of(
                        "example-configs/gradient-optimizer-config.json",
                        null // No expected output yet - test only ensures execution without errors
                        ),
                Arguments.of(
                        "example-configs/optimizer-config.json",
                        null // No expected output yet - test only ensures execution without errors
                        ));
    }

    /**
     * Tests the end-to-end functionality of default optimizer configurations.
     * This test ensures that default configurations execute without throwing errors.
     * If an expected prompt is provided (not null), it also validates the output matches expectations.
     * This design allows for gradual addition of expected outputs as needed.
     *
     * @param configPath     The path to the configuration file for the optimization run.
     * @param expectedPrompt The expected optimized prompt (optional - can be null for smoke testing only).
     */
    @ParameterizedTest
    @MethodSource("provideDefaultConfigTestCases")
    void testDefaultConfigExecutesWithoutErrors(String configPath, String expectedPrompt) throws Exception {
        File config = new File(configPath);
        Assertions.assertTrue(
                config.exists(), "The configuration file should exist at %s".formatted(config.getAbsolutePath()));

        Optimization optimization = new Optimization(config.toPath());
        List<String> optimizedPrompts = optimization.run();

        Assertions.assertFalse(
                optimizedPrompts.isEmpty(),
                "The optimizer returned no prompts; as maximum_iterations > 0 and that the optimizer produces at least"
                        + " one prompt.");

        // If expected prompt is provided, validate the output
        if (expectedPrompt != null) {
            String optimizedPrompt = optimizedPrompts.getLast();
            String escapedOptimizedPrompt = escapeMarkdown(optimizedPrompt);

            Assertions.assertEquals(
                    expectedPrompt.lines().map(String::strip).toList(),
                    escapedOptimizedPrompt.lines().map(String::strip).toList(),
                    "The optimized prompt does not match every line (regardless of line terminators or leading/trailing "
                            + "spaces) of the expected prompt.");
        }
    }
}
