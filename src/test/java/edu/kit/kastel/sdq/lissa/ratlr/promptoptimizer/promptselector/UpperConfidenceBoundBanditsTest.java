/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for the UpperConfidenceBoundBandits class.
 * <p>
 * These tests verify the core functionality of the UCB (Upper Confidence Bound) multi-armed bandit algorithm,
 * including arm selection strategy, score tracking, exploration-exploitation balance, and edge case handling.
 * <p>
 * Test categories:
 * <ul>
 * <li><b>Initialization:</b> Verifies correct setup of the bandit state</li>
 * <li><b>Updates:</b> Validates score and count accumulation</li>
 * <li><b>Arm Selection:</b> Tests the UCB algorithm's arm choice logic</li>
 * <li><b>Exploration-Exploitation:</b> Verifies balance between trying new arms and using known good ones</li>
 * <li><b>Edge Cases:</b> Handles boundary conditions and invalid inputs</li>
 * <li><b>Determinism:</b> Confirms reproducibility with fixed seeds</li>
 * </ul>
 */
@DisplayName("UpperConfidenceBoundBandits Unit Tests")
class UpperConfidenceBoundBanditsTest {

    private static final int NUM_ARMS = 5;
    private static final int NUM_SAMPLES = 10;
    private static final double EXPLORATION_CONSTANT = 1.0;
    private static final long FIXED_SEED = 42L;

    private UpperConfidenceBoundBandits bandit;

    @BeforeEach
    void setUp() {
        Random fixedRandom = new Random(FIXED_SEED);
        bandit = new UpperConfidenceBoundBandits(NUM_ARMS, NUM_SAMPLES, EXPLORATION_CONSTANT, "ucb", fixedRandom);
    }

    /**
     * Test that a newly initialized bandit has the correct number of arms.
     */
    @Test
    @DisplayName("Initialization creates correct number of arms")
    void shouldInitializeWithCorrectNumberOfArms() {
        double[] scores = bandit.getScores();
        Assertions.assertEquals(NUM_ARMS, scores.length, "Bandit should have exactly NUM_ARMS arms");
    }

    /**
     * Test that a newly initialized bandit has zero scores for all arms.
     */
    @Test
    @DisplayName("Initialization sets all scores to zero")
    void shouldInitializeAllScoresToZero() {
        double[] scores = bandit.getScores();
        for (int i = 0; i < NUM_ARMS; i++) {
            Assertions.assertEquals(
                    0.0, scores[i], 1e-9, "Score for arm " + i + " should be zero after initialization");
        }
    }

    /**
     * Test that updating selected arms with scores correctly updates their average score.
     */
    @Test
    @DisplayName("Update correctly computes average scores")
    void shouldUpdateArmsWithNewScores() {
        bandit.update(new int[] {0}, new double[] {0.8});
        double[] scores = bandit.getScores();

        Assertions.assertEquals(0.8, scores[0], 1e-9, "Arm 0 should have score 0.8");
    }

    /**
     * Test that updating multiple arms in one call works correctly.
     */
    @Test
    @DisplayName("Batch update applies to multiple arms")
    void shouldUpdateMultipleArmsInBatch() {
        bandit.update(new int[] {0, 2, 4}, new double[] {0.8, 0.9, 0.7});
        double[] scores = bandit.getScores();

        Assertions.assertEquals(0.8, scores[0], 1e-9, "Arm 0 should have score 0.8");
        Assertions.assertEquals(0.9, scores[2], 1e-9, "Arm 2 should have score 0.9");
        Assertions.assertEquals(0.7, scores[4], 1e-9, "Arm 4 should have score 0.7");
    }

    /**
     * Test that unupdated arms retain their zero score.
     */
    @Test
    @DisplayName("Unupdated arms remain at zero")
    void shouldLeaveUnupdatedArmsAtZero() {
        bandit.update(new int[] {0, 2}, new double[] {0.8, 0.9});
        double[] scores = bandit.getScores();

        Assertions.assertEquals(0.0, scores[1], "Arm 1 should remain at zero");
        Assertions.assertEquals(0.0, scores[3], "Arm 3 should remain at zero");
    }

    /**
     * Test that multiple consecutive updates to the same arm produce the correct average.
     */
    @Test
    @DisplayName("Multiple updates to same arm compute correct average")
    void shouldAccumulateScoresAsAverage() {
        // First update
        bandit.update(new int[] {0}, new double[] {0.8});
        double[] scoresAfterFirst = bandit.getScores();
        Assertions.assertEquals(0.8, scoresAfterFirst[0], 1e-9, "After first update, arm 0 should be 0.8");

        // Second update
        bandit.update(new int[] {0}, new double[] {0.6});
        double[] scoresAfterSecond = bandit.getScores();

        // Average of 0.8 and 0.6 should be 0.7
        Assertions.assertEquals(0.7, scoresAfterSecond[0], 1e-9, "Average of 0.8 and 0.6 should be 0.7");
    }

    /**
     * Test that scores approach their expected average over many updates.
     */
    @Test
    @DisplayName("Scores converge to average over many updates")
    void shouldConvergeToAccurateAverage() {
        double expectedAverage = 0.6;
        for (int i = 0; i < 100; i++) {
            bandit.update(new int[] {0}, new double[] {expectedAverage});
        }

        double[] scores = bandit.getScores();
        Assertions.assertEquals(
                expectedAverage, scores[0], 1e-9, "After 100 updates with same score, average should be exact");
    }

    /**
     * Test that update throws exception when chosen arms and scores arrays have different lengths.
     */
    @Test
    @DisplayName("Update throws when arrays have mismatched lengths")
    void shouldThrowExceptionWhenArrayLengthsMismatch() {
        int[] chosenArms = {0, 1};
        double[] scores = {0.8};

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> bandit.update(chosenArms, scores),
                "Should throw IllegalArgumentException for mismatched array lengths");
    }

    /**
     * Test that choosing from arms with no updates returns distinct random arms.
     * When all counts are zero, the bandit should select random distinct arms.
     */
    @Test
    @DisplayName("Choose with all-zero counts returns distinct arms")
    void shouldReturnDistinctArmsWhenAllCountsAreZero() {
        List<Integer> chosen = bandit.choose(3, 1);

        Assertions.assertEquals(3, chosen.size(), "Should choose exactly 3 arms");

        Set<Integer> uniqueArms = new HashSet<>(chosen);
        Assertions.assertEquals(3, uniqueArms.size(), "All chosen arms should be distinct");

        for (int arm : chosen) {
            Assertions.assertTrue(arm >= 0 && arm < NUM_ARMS, "Chosen arm " + arm + " should be a valid arm index");
        }
    }

    /**
     * Test that requesting more arms than available returns all unique arms (no duplicates).
     * When n > numberOfArms, the algorithm should return all arms exactly once.
     */
    @Test
    @DisplayName("Choose with n > numberOfArms returns all arms without duplicates")
    void shouldReturnAllArmsWhenNGreaterThanNumberOfArms() {
        List<Integer> chosen = bandit.choose(NUM_ARMS + 1, 1);

        Assertions.assertEquals(NUM_ARMS, chosen.size(), "Should return exactly NUM_ARMS arms, not NUM_ARMS + 1");

        Set<Integer> uniqueArms = new HashSet<>(chosen);
        Assertions.assertEquals(NUM_ARMS, uniqueArms.size(), "All returned arms should be unique");
    }

    /**
     * Test that the UCB algorithm selects the highest-scoring arm when there is
     * sufficient information to distinguish between arms. This verifies exploitation behavior.
     */
    @Test
    @DisplayName("Algorithm exploits highest-scoring arm with sufficient data")
    void shouldSelectHighestScoringArmWhenWellInformed() {
        // Give different scores to different arms
        bandit.update(new int[] {0, 1, 2, 3, 4}, new double[] {0.2, 0.8, 0.5, 0.1, 0.1});
        // Choose one arm at a high iteration (sufficient data for exploitation)
        List<Integer> chosen = bandit.choose(1, 100);
        // Arm 1 has the highest score (0.8), so it should be chosen
        Assertions.assertEquals(
                1, chosen.getFirst(), "Algorithm should exploit the highest-scoring arm (arm 1 with score 0.8)");
    }

    /**
     * Test that two bandits initialized with the same seed produce identical arm selections.
     * This verifies deterministic behavior and reproducibility.
     */
    @Test
    @DisplayName("Same seed produces deterministic results")
    void shouldProduceDeterministicResultsWithFixedSeed() {
        var bandit1 = new UpperConfidenceBoundBandits(
                NUM_ARMS, NUM_SAMPLES, EXPLORATION_CONSTANT, "ucb", new Random(FIXED_SEED));
        var bandit2 = new UpperConfidenceBoundBandits(
                NUM_ARMS, NUM_SAMPLES, EXPLORATION_CONSTANT, "ucb", new Random(FIXED_SEED));

        List<Integer> chosen1 = bandit1.choose(3, 1);
        List<Integer> chosen2 = bandit2.choose(3, 1);

        Assertions.assertEquals(chosen1, chosen2, "Two bandits with the same seed should make identical choices");
    }

    /**
     * Test that an invalid mode string throws an exception during initialization.
     */
    @Test
    @DisplayName("Invalid mode string throws exception")
    void shouldThrowExceptionForInvalidMode() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new UpperConfidenceBoundBandits(
                        NUM_ARMS, NUM_SAMPLES, EXPLORATION_CONSTANT, "invalid-mode", new Random(FIXED_SEED)),
                "Should throw IllegalArgumentException for unknown mode");
    }

    /**
     * Test that mode strings are case-insensitive.
     * Both "UCB" and "ucb" should be accepted as valid modes.
     */
    @ParameterizedTest
    @ValueSource(strings = {"UCB", "Ucb", "uCb", "ucB"})
    @DisplayName("Mode string is case-insensitive")
    void shouldAcceptModeStringInAnyCase(String mode) {
        var bandit = new UpperConfidenceBoundBandits(
                NUM_ARMS, NUM_SAMPLES, EXPLORATION_CONSTANT, mode, new Random(FIXED_SEED));

        List<Integer> chosen = bandit.choose(2, 1);
        Assertions.assertEquals(2, chosen.size(), "Should successfully initialize with mode: " + mode);
    }

    /**
     * Test that choosing with n=0 returns an empty list.
     */
    @Test
    @DisplayName("Choose with n=0 returns empty list")
    void shouldReturnEmptyListWhenNIsZero() {
        List<Integer> chosen = bandit.choose(0, 1);
        Assertions.assertEquals(0, chosen.size(), "Choosing 0 arms should return empty list");
    }

    /**
     * Test that getScores returns correct averages after multiple updates to same arm.
     */
    @Test
    @DisplayName("Score averaging works correctly with independent bandit instance")
    void shouldComputeCorrectAverageScores() {
        var testBandit = new UpperConfidenceBoundBandits(3, 1, EXPLORATION_CONSTANT, "ucb", new Random(FIXED_SEED));

        // Update arm 0 with scores 0.5, 0.7, 0.6
        testBandit.update(new int[] {0}, new double[] {0.5});
        testBandit.update(new int[] {0}, new double[] {0.7});
        testBandit.update(new int[] {0}, new double[] {0.6});

        double[] scores = testBandit.getScores();

        // Average should be (0.5 + 0.7 + 0.6) / 3 = 0.6
        Assertions.assertEquals(0.6, scores[0], 1e-9, "Average of [0.5, 0.7, 0.6] should be 0.6");
        Assertions.assertEquals(0.0, scores[1], "Arm 1 should have no updates");
        Assertions.assertEquals(0.0, scores[2], "Arm 2 should have no updates");
    }

    /**
     * Test batch update with multiple arms simultaneously with their respective scores.
     */
    @Test
    @DisplayName("Batch update applies to all specified arms")
    void shouldUpdateAllArmsInBatchCorrectly() {
        var testBandit = new UpperConfidenceBoundBandits(5, 10, EXPLORATION_CONSTANT, "ucb", new Random(FIXED_SEED));

        int[] chosenArms = {0, 1, 2, 3, 4};
        double[] scores = {0.1, 0.2, 0.3, 0.4, 0.5};

        testBandit.update(chosenArms, scores);

        double[] result = testBandit.getScores();

        for (int i = 0; i < 5; i++) {
            Assertions.assertEquals(scores[i], result[i], 1e-9, "Arm " + i + " should have score " + scores[i]);
        }
    }

    /**
     * Test that all returned arms are within valid indices across multiple iterations.
     */
    @Test
    @DisplayName("All chosen arms are within valid range")
    void shouldAlwaysReturnValidArmIndices() {
        bandit.update(new int[] {0, 1, 2, 3, 4}, new double[] {0.1, 0.2, 0.3, 0.4, 0.5});

        for (int iteration = 1; iteration <= 20; iteration++) {
            List<Integer> chosen = bandit.choose(3, iteration);
            for (int arm : chosen) {
                Assertions.assertTrue(
                        arm >= 0 && arm < NUM_ARMS, "Arm " + arm + " at iteration " + iteration + " is invalid");
            }
        }
    }

    /**
     * Test that exploration bonus allows under-explored arms to compete with well-explored but equal-score arms.
     * This tests the core exploration-exploitation tradeoff in UCB.
     */
    @Test
    @DisplayName("Exploration bonus favors under-explored arms")
    void shouldPreferUnderexploredArmsWhenScoresAreEqual() {
        var testBandit = new UpperConfidenceBoundBandits(2, 1, 2.0, "ucb", new Random(FIXED_SEED));

        // Both arms have the same average score
        testBandit.update(new int[] {0}, new double[] {0.5});
        testBandit.update(new int[] {1}, new double[] {0.5});
        testBandit.update(new int[] {1}, new double[] {0.5});

        // Arm 0 has been pulled 1 time, arm 1 has been pulled 2 times
        // With exploration bonus, arm 0 (under-explored) should be preferred
        List<Integer> chosen = testBandit.choose(1, 10);

        Assertions.assertEquals(
                0, chosen.getFirst(), "Should prefer arm 0 which is under-explored despite equal scores");
    }
}
