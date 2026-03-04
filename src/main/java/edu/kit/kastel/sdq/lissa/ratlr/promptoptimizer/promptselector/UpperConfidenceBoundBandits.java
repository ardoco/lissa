/* Licensed under MIT 2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector.Selector.DEFAULT_SEED;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Upper Confidence Bound (UCB) Bandits implements the UCB and UCB-E algorithms for multi-armed bandit problems.
 * This class maintains counts and scores for each arm (prompt) and selects arms based on the balance between
 * exploitation (choosing known good arms) and exploration (trying arms with high uncertainty).
 * <p>
 * The best arm identification approach and algorithms in multi-armed bandits is described in:
 * <blockquote>
 * Audibert, J.-Y., &amp; Bubeck, S. (2010).
 * Best Arm Identification in Multi-Armed Bandits.
 * In Proceedings of the 23rd Conference on Learning Theory (COLT-10) (pp. 13).
 * Haifa, Israel.
 * </blockquote>
 */
public class UpperConfidenceBoundBandits {

    /**
     * Epsilon value added to counts to prevent division by zero in UCB calculation.
     * This small constant ensures numerical stability when computing the exploration term.
     */
    private static final double EPSILON = 1e-3;

    private final double explorationConstant;
    private final Mode mode;
    private final int numberOfArms;
    private final int numberOfSamples;
    private final double[] counts;
    private final double[] scores;
    private final Random random;

    public UpperConfidenceBoundBandits(int numberOfArms, int numberOfSamples, double explorationConstant, String mode) {
        this.explorationConstant = explorationConstant;
        this.mode = Mode.fromString(mode);
        this.numberOfArms = numberOfArms;
        this.numberOfSamples = numberOfSamples;
        this.counts = new double[numberOfArms];
        this.scores = new double[numberOfArms];
        this.random = new Random(DEFAULT_SEED);
    }

    /**
     * Update the counts and scores for the chosen arms.
     * @param chosen An array of indices of the chosen arms.
     * @param scores An array of scores corresponding to the chosen arms.
     */
    public void update(int[] chosen, double[] scores) {
        for (int i = 0; i < chosen.length; i++) {
            int index = chosen[i];
            double score = scores[i];
            this.counts[index] += this.numberOfSamples;
            this.scores[index] += score * this.numberOfSamples;
        }
    }

    /**
     * Get the average scores for each arm.
     * @return An array of average scores for each arm.
     */
    public double[] getScores() {
        double[] result = new double[numberOfArms];
        for (int i = 0; i < numberOfArms; i++) {
            if (counts[i] != 0) {
                result[i] = scores[i] / counts[i];
            } else {
                result[i] = 0;
            }
        }
        return result;
    }

    /**
     * Choose the next set of arms to evaluate based on the UCB algorithm.
     * @param n The maximum number of arms to choose. If there are fewer arms than n, all arms will be chosen.
     * @param iteration The current round number. Used in the UCB formula to determine the level of exploration versus
     *          exploitation. Higher values of {@code iteration} increase the exploration term.
     * @return A list of indices of the chosen arms.
     */
    public List<Integer> choose(int n, int iteration) {
        // If all counts are 0, choose randomly.
        if (Arrays.equals(counts, new double[counts.length])) {
            return random.ints(0, numberOfArms).limit(n).boxed().toList();
        }

        double[] scores = new double[numberOfArms];
        double[] currentScores = getScores();
        for (int i = 0; i < numberOfArms; i++) {
            // Add epsilon to avoid division by zero in the exploration term
            double count = counts[i] + EPSILON;
            scores[i] = currentScores[i] + mode.computeExplorationTerm(explorationConstant, iteration, count);
        }
        return Arrays.stream(scores)
                .boxed()
                // sort in descending order
                .sorted((a, b) -> Double.compare(b, a))
                .limit(n)
                .map(score -> {
                    for (int i = 0; i < scores.length; i++) {
                        if (scores[i] == score) {
                            return i;
                        }
                    }
                    throw new IllegalStateException("Score not found: " + score + "\n This should never happen");
                })
                .toList();
    }

    /**
     * Enumeration of modes for the Upper Confidence Bound algorithm.
     * Each mode defines a different way to compute the exploration term in the UCB formula.
     */
    public enum Mode {
        /**
         * Standard UCB mode where the exploration term is computed as c * sqrt(log(iteration) / count).
         * This encourages exploration of arms that have been tried less frequently, especially in early rounds.
         */
        UCB("ucb") {
            @Override
            double computeExplorationTerm(double explorationConstant, int roundNumber, double count) {
                return explorationConstant * Math.sqrt(Math.log(roundNumber) / count);
            }
        },
        /**
         * UCB-E mode where the exploration term is computed as c * sqrt(c / count).
         * This variant emphasizes exploration more strongly, especially for arms that have been tried very few times.
         */
        UCB_E("ucb-e") {
            @Override
            double computeExplorationTerm(double explorationConstant, int roundNumber, double count) {
                return explorationConstant * Math.sqrt(explorationConstant / count);
            }
        };

        private final String modeName;

        /**
         * Constructor for the Mode enum.
         * @param modeName The string representation of the mode as used in configuration.
         */
        Mode(String modeName) {
            this.modeName = modeName;
        }

        /**
         * Gets the string representation of this mode.
         * @return The mode name as used in configuration.
         */
        public String getModeName() {
            return modeName;
        }

        /**
         * Computes the exploration term for the Upper Confidence Bound algorithm.
         * @param explorationConstant The exploration constant controlling the balance between exploration and exploitation.
         * @param roundNumber The current round number.
         * @param count The number of times this arm has been pulled.
         * @return The exploration term to add to the exploitation score.
         */
        abstract double computeExplorationTerm(double explorationConstant, int roundNumber, double count);

        /**
         * Converts a string mode name to the corresponding Mode enum value.
         * @param mode The string representation of the mode (case-insensitive).
         * @return The corresponding Mode enum value.
         * @throws IllegalArgumentException if the mode string does not match any known mode.
         */
        public static Mode fromString(String mode) {
            String normalizedMode = mode.toLowerCase();
            for (Mode m : Mode.values()) {
                if (m.modeName.equals(normalizedMode)) {
                    return m;
                }
            }
            throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }
}
