/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.reductor.MeanReductor;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.scorer.BinaryScorer;

/**
 * Tests for scorer and reductor implementations.
 * These tests focus on the individual components (scorers and reductors) rather than the full metrics.
 */
class ScorerReductorTest {

    // BinaryScorer tests

    /**
     * Tests the BinaryScorer with various classification tasks and results.
     * This test verifies that the scorer correctly identifies true positives, false positives, true negatives, and false negatives based on the presence of results and the expected labels.
     */
    @Test
    void testBinaryScorer_WithResult() {
        BinaryScorer scorer = new BinaryScorer();
        Element source = new Element("S1", "requirement", "Source", 0, null, true);
        Element target = new Element("T1", "requirement", "Target", 0, null, true);

        // TP: label=true, result present
        ClassificationTask taskTP = new ClassificationTask(source, target, true);
        assertEquals(1.0, scorer.score(taskTP, new ClassificationResult(source, target, 1.0)), 0.0001);

        // FP: label=false, result present
        ClassificationTask taskFP = new ClassificationTask(source, target, false);
        assertEquals(0.0, scorer.score(taskFP, new ClassificationResult(source, target, 1.0)), 0.0001);

        // TN: label=false, result present
        ClassificationTask taskTN = new ClassificationTask(source, target, false);
        assertEquals(1.0, scorer.score(taskTN, new ClassificationResult(source, target, 0.0)), 0.0001);

        // FN: label=true, result present
        ClassificationTask taskFN = new ClassificationTask(source, target, true);
        assertEquals(0.0, scorer.score(taskFN, new ClassificationResult(source, target, 0.0)), 0.0001);
    }

    /**
     * Tests the BinaryScorer when no classification result is provided.
     * This test verifies that the scorer defaults to 1.0 for true negatives (label=false) and 0.0 for false negatives (label=true) when no result is available.
     */
    @Test
    void testBinaryScorer_WithoutResult() {
        BinaryScorer scorer = new BinaryScorer();
        Element source = new Element("S1", "requirement", "Source", 0, null, true);
        Element target = new Element("T1", "requirement", "Target", 0, null, true);

        // TN: label=false, no result
        ClassificationTask taskTN = new ClassificationTask(source, target, false);
        assertEquals(1.0, scorer.score(taskTN, null), 0.0001);

        // FN: label=true, no result
        ClassificationTask taskFN = new ClassificationTask(source, target, true);
        assertEquals(0.0, scorer.score(taskFN, null), 0.0001);
    }

    /**
     * Tests that the BinaryScorer throws an exception when the number of tasks and results do not match.
     * This test verifies that the scorer correctly validates the input sizes and throws an IllegalArgumentException when there is a mismatch.
     */
    @Test
    void testBinaryScorer_ThrowsOnSizeMismatch() {
        BinaryScorer scorer = new BinaryScorer();
        Element s1 = new Element("S1", "requirement", "Source 1", 0, null, true);
        Element t1 = new Element("T1", "requirement", "Target 1", 0, null, true);

        List<ClassificationTask> tasks = List.of(new ClassificationTask(s1, t1, true));
        List<ClassificationResult> results = List.of();

        assertThrows(IllegalArgumentException.class, () -> scorer.score(tasks, results));
    }

    // MeanReductor tests

    /**
     * Tests the MeanReductor with various lists of scores.
     * This test verifies that the reductor correctly calculates the mean of the provided scores, handling edge cases such as an empty list and lists with uniform values.
     */
    @Test
    void testMeanReductor_CalculatesCorrectly() {
        MeanReductor reductor = new MeanReductor();

        assertEquals(0.0, reductor.reduce(List.of()), 0.0001);
        assertEquals(0.7, reductor.reduce(List.of(0.7)), 0.0001);
        assertEquals(0.6, reductor.reduce(List.of(1.0, 0.0, 1.0, 0.0, 1.0)), 0.0001);
        assertEquals(0.5, reductor.reduce(List.of(1.0, 1.0, 0.0, 0.0)), 0.0001);
    }

    // Integration test

    /**
     * Tests the integration of BinaryScorer and MeanReductor in a realistic scenario.
     * This test verifies that the scorer correctly computes scores for a set of classification tasks and results, and that the reductor correctly aggregates these scores to produce a final metric value.
     */
    @Test
    void testIntegration_BinaryScorerAndMeanReductor() {
        BinaryScorer scorer = new BinaryScorer();
        MeanReductor reductor = new MeanReductor();

        Element s1 = new Element("S1", "requirement", "Source 1", 0, null, true);
        Element s2 = new Element("S2", "requirement", "Source 2", 0, null, true);
        Element s3 = new Element("S3", "requirement", "Source 3", 0, null, true);
        Element s4 = new Element("S4", "requirement", "Source 4", 0, null, true);
        Element s5 = new Element("S5", "requirement", "Source 5", 0, null, true);
        Element t1 = new Element("T1", "requirement", "Target 1", 0, null, true);
        Element t2 = new Element("T2", "requirement", "Target 2", 0, null, true);
        Element t3 = new Element("T3", "requirement", "Target 3", 0, null, true);
        Element t4 = new Element("T4", "requirement", "Target 4", 0, null, true);
        Element t5 = new Element("T5", "requirement", "Target 5", 0, null, true);

        List<ClassificationTask> tasks = List.of(
                new ClassificationTask(s1, t1, true),
                new ClassificationTask(s2, t2, true),
                new ClassificationTask(s3, t3, true),
                new ClassificationTask(s4, t4, false),
                new ClassificationTask(s5, t5, false));

        List<ClassificationResult> results = List.of(
                new ClassificationResult(s1, t1, 1.0),
                new ClassificationResult(s2, t2, 1.0),
                new ClassificationResult(s3, t3, 1.0),
                new ClassificationResult(s4, t4, 1.0),
                new ClassificationResult(s5, t5, 1.0));

        List<Double> scores = scorer.score(tasks, results);
        double finalScore = reductor.reduce(scores);

        // 3 correct (1.0) + 2 incorrect (0.0) = 0.6
        assertEquals(0.6, finalScore, 0.0001);
    }
}
