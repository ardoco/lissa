/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.MockClassifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.context.ContextStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

/**
 * Tests for global metrics including FBetaMetric and MockMetric.
 * Global metrics evaluate classification tasks as a whole set rather than individually.
 */
class MetricTest {

    private List<ClassificationTask> testTasks;

    @BeforeEach
    void setUp() {
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

    /**
     * Helper method to create a ModuleConfiguration for FBetaMetric with given beta value.
     */
    static ModuleConfiguration createFBetaConfig(String beta) {
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("beta", beta);
        return new ModuleConfiguration("f-beta", configMap);
    }

    /**
     * Helper method to create a ModuleConfiguration for a given scorer and reductor combination.
     */
    private static ModuleConfiguration createPointwiseConfig(String scorerName, String reductorName) {
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("metric", scorerName);
        configMap.put("reductor", reductorName);
        return new ModuleConfiguration("pointwise", configMap);
    }

    /**
     * Provides different global metric instances for parameterized testing.
     * To add new global metrics, simply add them to the list.
     *
     * @return Stream of Metric instances for testing
     */
    private static Stream<Arguments> metrics() {
        ContextStore contextStore = new ContextStore();
        MockClassifier mockClassifier = new MockClassifier(contextStore);

        return Stream.of(
                Arguments.of(new FBetaMetric(createFBetaConfig("1"), mockClassifier, null, null)),
                Arguments.of(new FBetaMetric(createFBetaConfig("2"), mockClassifier, null, null)),
                Arguments.of(new PointwiseMetric(createPointwiseConfig("binary", "mean"), mockClassifier)));
    }

    // Generic tests for all Metric implementations

    /**
     * Tests that the metric returns a valid score for a given prompt and task list.
     * It checks that the score is not null and falls within the expected range defined by Metric.MINIMUM_SCORE and Metric.MAXIMUM_SCORE.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_ReturnsValidScore(Metric metric) {
        String metricName = metric.getName();
        assertNotNull(metricName, "Metric name should not be null");

        String prompt = "Test prompt for " + metricName;
        Double score = metric.getMetric(prompt, testTasks);

        assertNotNull(score, "Score should not be null");
        assertTrue(
                score >= Metric.MINIMUM_SCORE && score <= Metric.MAXIMUM_SCORE,
                "Score should be between " + Metric.MINIMUM_SCORE + " and " + Metric.MAXIMUM_SCORE);
    }

    /**
     * Tests that the metric can handle multiple prompts and returns a list of scores corresponding to each prompt.
     * It checks that the scores list is not null, has the same size as the prompts list, and that all scores are consistent for the same tasks and MockClassifier.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_MultiplePrompts(Metric metric) {
        List<String> prompts = List.of("First test prompt", "Second test prompt", "Third test prompt");
        List<Double> scores = metric.getMetric(prompts, testTasks);

        assertNotNull(scores, "Scores list should not be null");
        assertEquals(prompts.size(), scores.size(), "Should have one score per prompt");
        assertTrue(
                scores.stream().allMatch(score -> score.equals(scores.getFirst())),
                "All scores should be the same for the same tasks and MockClassifier");
        Double score = scores.getFirst();
        assertNotNull(score, "Score at any index should not be null");
    }

    /**
     * Tests that the metric can handle an empty list of classification tasks without throwing exceptions.
     * It checks that the score is not null and returns the expected minimum score for an empty task list.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_EmptyTaskList(Metric metric) {
        String prompt = "Test prompt with empty tasks";
        List<ClassificationTask> emptyTasks = List.of();
        Double score = metric.getMetric(prompt, emptyTasks);

        assertNotNull(score, "Score should not be null even with empty task list");
        assertEquals(Metric.MINIMUM_SCORE, score, "Expected score of " + Metric.MINIMUM_SCORE + " for empty task list");
    }

    /**
     * Tests that the metric can handle a single classification task and returns a valid score.
     * It checks that the score is not null and returns the expected score based on the label of the single task and the behavior of MockClassifier.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_SingleTask(Metric metric) {
        List<ClassificationTask> singleTask = testTasks.stream()
                .filter(ClassificationTask::label)
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new IllegalStateException("No task with label=true found in testTasks"));

        String prompt = "Test prompt with single task";
        Double score = metric.getMetric(prompt, singleTask);

        assertNotNull(score, "Score should not be null for single task");
        // Single task with label=true, MockClassifier returns positive, so perfect score
        assertEquals(
                Metric.MAXIMUM_SCORE,
                score,
                "Expected perfect score of " + Metric.MAXIMUM_SCORE + " for single correct classification");
        singleTask = testTasks.stream()
                .filter(task -> !task.label())
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new IllegalStateException("No task with label=false found in testTasks"));
        score = metric.getMetric(prompt, singleTask);
        assertNotNull(score, "Score should not be null for single task");
        // Single task with label=false, MockClassifier returns positive (false positive), so score is 0.0
        assertEquals(
                Metric.MINIMUM_SCORE,
                score,
                "Expected score of " + Metric.MINIMUM_SCORE + " when no true positives exist");
    }

    /**
     * Tests that the metric returns consistent scores for the same prompt and task list across multiple calls.
     * It checks that the scores are not null and that they are equal, ensuring that the metric is deterministic for the same input.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_ConsistencyAcrossMultipleCalls(Metric metric) {
        String prompt = "Consistency test prompt";
        Double score1 = metric.getMetric(prompt, testTasks);
        Double score2 = metric.getMetric(prompt, testTasks);

        assertNotNull(score1, "First score should not be null");
        assertNotNull(score2, "Second score should not be null");
        assertEquals(score1, score2, "Same prompt and tasks should yield consistent scores");
    }

    /**
     * Tests that the metric can handle cases where all classification tasks have positive labels (true) and returns the expected score.
     * It checks that the score is not null and returns the expected perfect score since MockClassifier returns positive for all tasks.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_WithOnlyPositiveLabels(Metric metric) {
        // Create tasks with only positive labels
        List<ClassificationTask> positiveTasks =
                testTasks.stream().filter(ClassificationTask::label).toList();

        String prompt = "Test prompt with only positive labels";
        Double score = metric.getMetric(prompt, positiveTasks);

        assertNotNull(score, "Score should not be null");
        // All tasks have label=true and MockClassifier returns positive, so perfect score expected
        assertEquals(
                Metric.MAXIMUM_SCORE,
                score,
                "Expected perfect score of " + Metric.MAXIMUM_SCORE + " for single correct classification");
    }

    /**
     * Tests that the metric can handle cases where all classification tasks have negative labels (false) and returns the expected score.
     * It checks that the score is not null and returns the expected minimum score since MockClassifier returns positive for all tasks, resulting in false positives.
     *
     * @param metric The Metric instance to test
     */
    @ParameterizedTest
    @MethodSource("metrics")
    void testMetric_WithOnlyNegativeLabels(Metric metric) {
        List<ClassificationTask> negativeTasks =
                testTasks.stream().filter(task -> !task.label()).toList();

        String prompt = "Test prompt with only negative labels";
        Double score = metric.getMetric(prompt, negativeTasks);

        assertNotNull(score, "Score should not be null");
        // All tasks have label=false but MockClassifier returns positive (all false positives, no true positives)
        assertEquals(
                Metric.MINIMUM_SCORE,
                score,
                "Expected score of " + Metric.MINIMUM_SCORE + " when all predictions are false positives");
    }
}
