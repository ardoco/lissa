/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.evaluator;

import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;

/**
 * Abstract base class for evaluators in the LiSSA framework.
 * This class provides the foundation for implementing different evaluation strategies
 * for prompts in classification tasks.
 * TODO: What are evaluators exactly?
 */
public abstract class Evaluator {

    /**
     * Default seed for random number generation to ensure reproducibility.
     */
    protected static final String SEED_KEY = "seed";

    protected static final int DEFAULT_SEED = 42069;
    private static final String SAMPLES_PER_EVAL_KEY = "samples_per_eval";
    private static final int SAMPLES_PER_EVAL = 32;
    private static final String EVAL_ROUNDS_KEY = "eval_rounds";
    private static final int EVAL_ROUNDS = 8;
    private static final String EVAL_PROMPTS_PER_ROUND_KEY = "eval_prompts_per_round";
    private static final int EVAL_PROMPTS_PER_ROUND = 8;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected int samplesPerEval;
    protected int evalRounds;
    protected int evalPromptsPerRound;
    protected final int evaluationBudget;
    protected final RandomGenerator random;

    protected Evaluator(ModuleConfiguration configuration) {
        this.samplesPerEval = configuration.argumentAsInt(SAMPLES_PER_EVAL_KEY, SAMPLES_PER_EVAL);
        this.evalRounds = configuration.argumentAsInt(EVAL_ROUNDS_KEY, EVAL_ROUNDS);
        this.evalPromptsPerRound = configuration.argumentAsInt(EVAL_PROMPTS_PER_ROUND_KEY, EVAL_PROMPTS_PER_ROUND);
        this.evaluationBudget = samplesPerEval * evalRounds * evalPromptsPerRound;
        this.random = new Random(configuration.argumentAsInt(SEED_KEY, DEFAULT_SEED));
    }

    /**
     * Factory method to create an evaluator based on the provided configuration.
     * The name field indicates the type of evaluator to create.
     * If the configuration is null, a MockEvaluator is returned by default.
     *
     * @param configuration The configuration specifying the type of evaluator to create.
     * @return An instance of a concrete evaluator implementation.
     * @throws IllegalStateException If the configuration name does not match any known evaluator types.
     */
    public static Evaluator createEvaluator(ModuleConfiguration configuration) {
        if (configuration == null) {
            return new MockEvaluator();
        }
        return switch (configuration.name()) {
            case "mock" -> new MockEvaluator();
            case "bruteforce" -> new BruteForceEvaluator(configuration);
            case "ucb" -> new UpperConfidenceBoundBanditEvaluator(configuration);
            default -> throw new IllegalStateException("Unexpected value: " + configuration.name());
        };
    }

    /**
     * Evaluates a list of prompts using the provided classification tasks, classifier, and metric.
     * TODO: name the method
     *
     * @param prompts A list of prompts to evaluate.
     * @param examples A list of classification task examples.
     * @param metric The metric instance to use for scoring the prompts.
     * @return A list of scores corresponding to each prompt.
     */
    public abstract List<Double> sampleAndEvaluate(
            List<String> prompts, List<ClassificationTask> examples, Metric metric);
}
