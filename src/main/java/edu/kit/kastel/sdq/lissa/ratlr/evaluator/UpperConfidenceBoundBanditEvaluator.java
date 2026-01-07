/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;

/**
 * An evaluator that uses the Upper Confidence Bound (UCB) algorithm to select prompts
 * for evaluation in a multi-armed bandit setting.
 * This evaluator iteratively selects prompts based on their performance, balancing exploration
 * and exploitation to optimize the evaluation process.
 * The UCB algorithm is particularly useful in scenarios where the goal is to maximize the cumulative reward
 * over a series of selections, making it well-suited for prompt optimization tasks.
 * <br>
 * Default parameters:
 * <ul>
 *   <li>rounds: 40</li>
 *   <li>numPromptsPerRound: 10</li>
 *   <li>samplesPerEval: 5</li>
 *   <li>maxThreads: 1</li>
 *   <li>c: 1.0</li>
 *   <li>mode: "ucb"</li>
 * </ul>
 */
public class UpperConfidenceBoundBanditEvaluator extends Evaluator {

    private static final String ROUNDS_KEY = "rounds";
    private static final int DEFAULT_ROUNDS = 40;
    private static final String NUM_PROMPTS_PER_ROUND_KEY = "num_prompts_per_round";
    private static final int DEFAULT_NUM_PROMPTS_PER_ROUND = 10;
    private static final String MAX_THREADS_KEY = "max_threads";
    private static final int DEFAULT_MAX_THREADS = 1;
    private static final String C_KEY = "c";
    private static final double DEFAULT_C = 1.0;
    private static final String MODE_KEY = "mode";
    private static final String DEFAULT_MODE = "ucb";

    private final int rounds;
    private final int numberOfPromptsPerRound;
    private final int maxThreads;
    private final double c;
    private final String mode;

    /**
     * Creates a new UpperConfidenceBoundBanditEvaluator instance with the given configuration.
     *
     * @param configuration The configuration for the evaluator.
     */
    public UpperConfidenceBoundBanditEvaluator(ModuleConfiguration configuration) {
        super(configuration);
        this.rounds = configuration.argumentAsInt(ROUNDS_KEY, DEFAULT_ROUNDS);
        this.numberOfPromptsPerRound =
                configuration.argumentAsInt(NUM_PROMPTS_PER_ROUND_KEY, DEFAULT_NUM_PROMPTS_PER_ROUND);
        this.maxThreads = configuration.argumentAsInt(MAX_THREADS_KEY, DEFAULT_MAX_THREADS);
        this.c = configuration.argumentAsDouble(C_KEY, DEFAULT_C);
        this.mode = configuration.argumentAsString(MODE_KEY, DEFAULT_MODE);
    }

    @Override
    public List<Double> sampleAndEvaluate(List<String> prompts, List<ClassificationTask> examples, Metric metric) {
        UpperConfidenceBoundBandits banditAlgo =
                new UpperConfidenceBoundBandits(prompts.size(), this.samplesPerEval, this.c, this.mode);
        int numPromptsPerRound = Math.min(this.numberOfPromptsPerRound, prompts.size());
        for (int ri = 1; ri <= this.rounds; ri++) {
            // Sample the prompts
            List<Integer> sampledPromptsIdx = banditAlgo.choose(numPromptsPerRound, ri);
            List<String> sampledPrompts = new ArrayList<>();
            for (int idx : sampledPromptsIdx) {
                sampledPrompts.add(prompts.get(idx));
            }
            List<ClassificationTask> sampledData = dataSampler(examples, this.samplesPerEval);
            List<Double> scores;
            while (true) {
                try {
                    // TODO: Pryzant et al. used multiple threads here
                    scores = metric.getMetric(sampledPrompts, sampledData);
                    break;
                } catch (Exception e) {
                    logger.warn("Exception during scoring: {}. Retrying...", e.getMessage());
                }
            }
            int[] chosenArray = sampledPromptsIdx.stream().mapToInt(i -> i).toArray();
            double[] scoresArray = scores.stream().mapToDouble(i -> i).toArray();
            banditAlgo.update(chosenArray, scoresArray);
        }
        return Arrays.stream(banditAlgo.getScores()).boxed().toList();
    }

    private <T> List<T> dataSampler(List<T> list, int samplesPerEval) {
        return new ArrayList<>(list.subList(0, Math.min(samplesPerEval, list.size())));
    }

    /**
     * Upper Confidence Bound (UCB) Bandits
     * Implements the UCB and UCB-E algorithms for multi-armed bandit problems.
     * This class maintains counts and scores for each arm (prompt) and selects arms
     */
    private static class UpperConfidenceBoundBandits {
        private final double c;
        private final String mode;
        private final int numPrompts;
        private final int numSamples;
        private final double[] counts;
        private final double[] scores;
        private final Random random;

        public UpperConfidenceBoundBandits(int numPrompts, int numSamples, double c, String mode) {
            this.c = c;
            if (!mode.equals("ucb") && !mode.equals("ucb-e")) {
                throw new IllegalArgumentException("Mode must be 'ucb' or 'ucb-e'");
            }
            this.mode = mode;
            this.numPrompts = numPrompts;
            this.numSamples = numSamples;
            this.counts = new double[numPrompts];
            this.scores = new double[numPrompts];
            this.random = new Random(DEFAULT_SEED);
        }

        /**
         * Update the counts and scores for the chosen prompts.
         * @param chosen An array of indices of the chosen prompts.
         * @param scores An array of scores corresponding to the chosen prompts.
         */
        public void update(int[] chosen, double[] scores) {
            for (int i = 0; i < chosen.length; i++) {
                int index = chosen[i];
                double score = scores[i];
                this.counts[index] += this.numSamples;
                this.scores[index] += score * this.numSamples;
            }
        }

        /**
         * Get the average scores for each prompt.
         * @return An array of average scores for each prompt.
         */
        public double[] getScores() {
            double[] result = new double[numPrompts];
            for (int i = 0; i < numPrompts; i++) {
                if (counts[i] != 0) {
                    result[i] = scores[i] / counts[i];
                } else {
                    result[i] = 0;
                }
            }
            return result;
        }

        /**
         * Choose the next set of prompts to evaluate based on the UCB algorithm.
         * @param n The number of prompts to choose.
         * @param t The current round number. Used in the UCB formula to determine the level of exploration versus
         *          exploitation. Higher values of {@code t} increase the exploration term.
         * @return A list of indices of the chosen prompts.
         */
        public List<Integer> choose(int n, int t) {
            // If all counts are 0, choose randomly.
            if (Arrays.equals(counts, new double[counts.length])) {
                return random.ints(0, numPrompts).limit(n).boxed().toList();
            }

            double[] ucbScores = new double[numPrompts];
            double[] currentScores = getScores();
            for (int i = 0; i < numPrompts; i++) {
                // to avoid division by zero
                double count = counts[i] + 1e-3;
                if (mode.equals("ucb")) {
                    ucbScores[i] = currentScores[i] + c * Math.sqrt(Math.log(t) / count);
                } else if (mode.equals("ucb-e")) {
                    ucbScores[i] = currentScores[i] + c * Math.sqrt(c / count);
                }
            }
            return Arrays.stream(ucbScores)
                    .boxed()
                    // sort in descending order
                    .sorted((a, b) -> Double.compare(b, a))
                    .limit(n)
                    .map(score -> {
                        for (int i = 0; i < ucbScores.length; i++) {
                            if (ucbScores[i] == score) {
                                return i;
                            }
                        }
                        // should never happen
                        return -1;
                    })
                    .toList();
        }
    }
}
