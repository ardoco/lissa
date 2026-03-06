/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeOptimizer.SAMPLER_CONFIGURATION_KEY;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy.SHUFFLED_SAMPLER;

import java.util.List;
import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy;

/**
 * A selector that performs a simple evaluation of all provided prompts.
 * All prompts are selected evenly for evaluation with the same deterministically random subset of classification tasks.
 */
public class SimpleSelector implements Selector {

    private static final int EVALUATION_ROUNDS = 8;
    private static final int EVALUATION_PROMPTS_PER_ROUND = 8;

    private final int evaluationBudget;
    private final SampleStrategy sampleStrategy;

    /**
     * Creates a new brute-force selector instance with the given configuration.
     *
     * @param configuration The configuration for the selector.
     */
    public SimpleSelector(ModuleConfiguration configuration) {
        int samplesPerEval = configuration.argumentAsInt(SAMPLES_PER_EVALUATION_CONFIGURATION_KEY, SAMPLES_PER_EVAL);
        int evalRounds = configuration.argumentAsInt("eval_rounds", EVALUATION_ROUNDS);
        int evalPromptsPerRound = configuration.argumentAsInt("eval_prompts_per_round", EVALUATION_PROMPTS_PER_ROUND);
        this.evaluationBudget = samplesPerEval * evalRounds * evalPromptsPerRound;
        this.sampleStrategy = SampleStrategy.createSampler(
                configuration.argumentAsString(SAMPLER_CONFIGURATION_KEY, SHUFFLED_SAMPLER),
                new Random(configuration.argumentAsInt("seed", DEFAULT_SEED)));
    }

    /**
     * Evaluates all provided prompts using the given metric.
     * It selects a subset of classification tasks using the internal {@link SampleStrategy} limited by the evaluation
     * budget and number of prompts.
     * Each prompt is evaluated against all selected tasks, and the scores computed through the metric are returned.
     */
    @Override
    public List<Double> selectAndEvaluate(
            List<String> prompts, List<ClassificationTask> classificationTasks, Metric metric) {
        int sampleSize = Math.min(classificationTasks.size(), (this.evaluationBudget / prompts.size()));
        List<ClassificationTask> classificationExamples = sampleStrategy.sample(classificationTasks, sampleSize);
        return metric.getMetric(prompts, classificationExamples);
    }
}
