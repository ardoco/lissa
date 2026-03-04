/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeOptimizer.SAMPLER_CONFIGURATION_KEY;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SamplerFactory.FIRST_SAMPLER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SamplerFactory;

/**
 * A selector that uses the Upper Confidence Bound (UCB) algorithm to select prompts
 * for evaluation in a multi-armed bandit setting.
 * This selector iteratively selects prompts based on their performance, balancing exploration
 * and exploitation to optimize the evaluation process.
 * <p>
 * The exploration constant {@code c} controls the trade-off between exploration (trying prompts with high uncertainty)
 * and exploitation (choosing prompts with known high performance). Higher values of {@code c} lead to more exploration.
 * <p>
 * This implementation is based on the approach used by Pryzant et al. (2023) for automatic prompt optimization.
 * <br>
 * Default parameters:
 * <ul>
 *   <li>rounds: 40</li>
 *   <li>numPromptsPerRound: 10</li>
 *   <li>samplesPerEval: 5</li>
 *   <li>c: 1.0 (exploration constant)</li>
 *   <li>mode: "ucb"</li>
 * </ul>
 */
public class UpperConfidenceBoundBanditSelector implements Selector {

    /**
     * Number of rounds to perform in the UCB algorithm. In each round, a subset of prompts is selected and evaluated.
     */
    private static final int DEFAULT_ROUNDS = 40;
    /**
     * Number of prompts to select and evaluate in each round. This controls how many prompts are evaluated per iteration.
     */
    private static final int DEFAULT_NUMBER_OF_PROMPTS_PER_ROUND = 10;
    /**
     * Exploration constant used in the UCB algorithm to balance exploration and exploitation. Higher values lead to more exploration.
     */
    private static final double DEFAULT_EXPLORATION_CONSTANT = 1.0;

    private final int samplesPerEvaluation;
    private final int rounds;
    private final int numberOfPromptsPerRound;
    private final double explorationConstant;
    private final String mode;
    private final SampleStrategy sampleStrategy;

    /**
     * Creates a new UpperConfidenceBoundBanditSelector instance with the given configuration.
     *
     * @param configuration The configuration for the selector.
     */
    public UpperConfidenceBoundBanditSelector(ModuleConfiguration configuration) {
        this.samplesPerEvaluation =
                configuration.argumentAsInt(SAMPLES_PER_EVALUATION_CONFIGURATION_KEY, SAMPLES_PER_EVAL);
        this.rounds = configuration.argumentAsInt("rounds", DEFAULT_ROUNDS);
        this.numberOfPromptsPerRound =
                configuration.argumentAsInt("num_prompts_per_round", DEFAULT_NUMBER_OF_PROMPTS_PER_ROUND);
        this.explorationConstant = configuration.argumentAsDouble("c", DEFAULT_EXPLORATION_CONSTANT);
        this.mode = configuration.argumentAsString("mode", UpperConfidenceBoundBandits.Mode.UCB.getModeName());

        this.sampleStrategy = SamplerFactory.createSampler(
                configuration.argumentAsString(SAMPLER_CONFIGURATION_KEY, FIRST_SAMPLER),
                new Random(configuration.argumentAsInt("seed", DEFAULT_SEED)));
    }

    /**
     * Selects prompts using the Upper Confidence Bound (UCB) algorithm and evaluates them using the provided metric.
     * The method iteratively selects a subset of prompts based on their performance, evaluates them on a sampled set of classification tasks,
     * and updates the UCB algorithm with the obtained scores to refine future selections.
     */
    @Override
    public List<Double> selectAndEvaluate(List<String> prompts, List<ClassificationTask> examples, Metric metric) {
        UpperConfidenceBoundBandits banditAlgo = new UpperConfidenceBoundBandits(
                prompts.size(), this.samplesPerEvaluation, this.explorationConstant, this.mode);
        for (int round = 1; round <= this.rounds; round++) {
            // Sample the prompts
            List<Integer> sampledPromptIdentifiers = banditAlgo.choose(numberOfPromptsPerRound, round);
            List<String> sampledPrompts = new ArrayList<>();
            for (int identifier : sampledPromptIdentifiers) {
                sampledPrompts.add(prompts.get(identifier));
            }
            List<ClassificationTask> sampledData = sampleStrategy.sample(examples, this.samplesPerEvaluation);

            List<Double> scores = metric.getMetric(sampledPrompts, sampledData);
            // Update the bandit algorithm with the obtained scores
            int[] chosenArray =
                    sampledPromptIdentifiers.stream().mapToInt(i -> i).toArray();
            double[] scoresArray = scores.stream().mapToDouble(i -> i).toArray();
            banditAlgo.update(chosenArray, scoresArray);
        }
        return Arrays.stream(banditAlgo.getScores()).boxed().toList();
    }
}
