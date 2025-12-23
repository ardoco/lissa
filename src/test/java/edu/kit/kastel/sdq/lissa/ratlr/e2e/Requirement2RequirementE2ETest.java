/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.e2e;

import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.escapeMarkdown;
import static edu.kit.kastel.sdq.lissa.ratlr.Statistics.getTraceLinksFromGoldStandard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        // TODO TEMPORARY: Only run feedback optimizer test
        return Stream.of(Arguments.of(
                "src/test/resources/warc/WARC_feedback_gpt_gpt-4o-mini-2024-07-18_0_mi5_fs3.json",
                Files.readString(Path.of("src/test/resources/expected/FeedbackOptimizerExpectation.txt"))));

        /* COMMENTED OUT - Restore after debugging
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
        */
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
        String optimizedPrompt = optimization.run();
        String escapedOptimizedPrompt = escapeMarkdown(optimizedPrompt);

        Assertions.assertEquals(
                expectedPrompt.lines().map(String::strip).toList(),
                escapedOptimizedPrompt.lines().map(String::strip).toList(),
                "The optimized prompt does not match every line (regardless of line terminators or leading/trailing "
                        + "spaces) of the expected prompt.");
    }
}
