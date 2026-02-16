/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import static edu.kit.kastel.sdq.lissa.ratlr.promptmetric.MetricTest.createFBetaConfig;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.MockClassifier;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Tests for global metrics including FBetaMetric and MockMetric.
 * Global metrics evaluate classification tasks as a whole set rather than individually.
 */
class GlobalMetricTest {

    private MockClassifier mockClassifier;
    private List<ClassificationTask> testTasks;

    @BeforeEach
    void setUp() {
        ContextStore contextStore = new ContextStore();
        mockClassifier = new MockClassifier(contextStore);

        // Create test elements
        Element source1 = new Element("S1", "requirement", "Source requirement 1", 0, null, true);
        Element source2 = new Element("S2", "requirement", "Source requirement 2", 0, null, true);
        Element source3 = new Element("S3", "requirement", "Source requirement 3", 0, null, true);
        Element target1 = new Element("T1", "requirement", "Target requirement 1", 0, null, true);
        Element target2 = new Element("T2", "requirement", "Target requirement 2", 0, null, true);
        Element target3 = new Element("T3", "requirement", "Target requirement 3", 0, null, true);

        // Create diverse classification tasks
        testTasks = List.of(
                new ClassificationTask(source1, target1, true), // should be linked
                new ClassificationTask(source1, target2, true), // should be linked
                new ClassificationTask(source2, target1, false), // should not be linked
                new ClassificationTask(source2, target2, false), // should not be linked
                new ClassificationTask(source3, target3, true) // should be linked
                );
    }

    // FBetaMetric-specific calculation tests

    /**
     * Provides test cases for F-beta score calculations with different beta values.
     * Each test case contains a beta value and its expected F-score result.
     */
    private static Stream<Arguments> fBetaTestCases() {
        return Stream.of(
                Arguments.of("1", 0.75), // F1 = 2 * (0.6 * 1.0) / (0.6 + 1.0) = 0.75
                Arguments.of("2", 0.8823529411764706) // F2 = 5 * (0.6 * 1.0) / (2.4 + 1.0) ≈ 0.8824
                );
    }

    /**
     * Tests the calculation of F-beta score for different beta values.
     * For testTasks: 3 true labels, 2 false labels.
     * MockClassifier returns positive for all: TP=3, FP=2, FN=0
     * Precision = 3/5 = 0.6, Recall = 3/3 = 1.0
     */
    @ParameterizedTest
    @MethodSource("fBetaTestCases")
    void testFBetaMetric_CalculatesCorrectScore(String beta, double expectedScore) {
        FBetaMetric metric = new FBetaMetric(createFBetaConfig(beta), mockClassifier, null, null);

        String prompt = "Calculate F-beta score";
        Double score = metric.getMetric(prompt, testTasks);

        assertNotNull(score, "Score should not be null");
        assertEquals(
                expectedScore,
                score,
                0.0001,
                "F%s-Score should be %.4f for this scenario".formatted(beta, expectedScore));
    }
}
