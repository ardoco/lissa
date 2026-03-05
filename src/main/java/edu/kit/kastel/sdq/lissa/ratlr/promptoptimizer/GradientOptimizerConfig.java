/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeFeedbackOptimizer.FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeOptimizer.SAMPLER_CONFIGURATION_KEY;

import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector.Selector;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SamplerFactory;

/**
 * Configuration class for the gradient-based prompt optimizer.
 * This class encapsulates all configuration parameters required for the optimization process,
 * including prompt templates, sampling strategies, and evaluation settings.
 *
 * @param numberOfGradients The total number of gradient steps to perform during optimization (default: {@value DEFAULT_NUMBER_OF_GRADIENTS})
 * @param maximumErrorExamples The maximum number of misclassified examples to consider when filtering candidate prompts (default: {@value DEFAULT_MAX_ERROR_EXAMPLES})
 * @param numberOfErrors The number of misclassified examples to use for generating feedback in each iteration (default: {@value DEFAULT_NUMBER_OF_ERRORS})
 * @param numberOfGradientsPerError The number of gradient steps to perform for each misclassified example (default: {@value DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR})
 * @param stepsPerGradient The number of optimization steps to perform for each generated prompt (default: {@value DEFAULT_STEPS_PER_GRADIENT})
 * @param monteCarloSamplesPerStep The number of Monte Carlo samples to use for evaluating each candidate prompt during
 *                         optimization (default: {@value DEFAULT_MC_SAMPLES_PER_STEP})
 * @param maximumExpansionFactor The maximum expansion factor for the number of candidate prompts generated in each
 *                           iteration (relative to the number of misclassified examples) (default: {@value DEFAULT_MAX_EXPANSION_FACTOR})
 * @param rejectOnErrors Whether to reject candidate prompts that still misclassify examples after optimization steps (default: {@value DEFAULT_REJECT_ON_ERRORS})
 * @param evaluationBudget The total number of examples to use for evaluating candidate prompts during optimization
 *                         (used to limit the number of evaluations and control runtime) (default: 2048, calculated as {@value DEFAULT_SAMPLES_PER_EVAL} * {@value DEFAULT_EVAL_ROUNDS} * {@value DEFAULT_EVAL_PROMPTS_PER_ROUND})
 * @param minibatchSize The size of the minibatches to use when evaluating candidate prompts on the misclassified
 *                      examples (used to control memory usage and evaluation time) (default: {@value DEFAULT_MINIBATCH_SIZE})
 * @param beamSize The beam size to use when selecting the best candidate prompts during optimization (default: {@value DEFAULT_BEAM_SIZE})
 * @param candidateOversamplingFactor Oversampling factor for candidate filtering. Samples this many times the expansion factor
 *                                    before evaluating on errors to reduce evaluation cost (default: {@value DEFAULT_CANDIDATE_OVERSAMPLING_FACTOR})
 * @param gradientPrompt The prompt template used for generating feedback on misclassified examples during the gradient
 *                       optimization process
 * @param transformationPrompt The prompt template used for generating candidate prompt transformations based on
 *                            the feedback from the gradient optimization process
 * @param synonymPrompt The prompt template used for generating synonym variations of the current prompt, to introduce
 *                      additional diversity in the candidate prompts during optimization
 * @param feedbackExampleBlock The template used for formatting misclassified examples and their feedback when
 *                             generating candidate prompts
 * @param candidateEvaluationSelector The selector to use for evaluating candidate prompts on full task sets
 * @param minibatchSampler The sampling strategy to use for minibatch selection during candidate expansion rounds
 * @param errorSampler The sampling strategy to use when sampling error examples for gradient generation
 * @param candidateFilterSampler The sampling strategy to use when filtering candidate prompts
 * @param errorEvaluationSelector The selector to use when evaluating filtered candidate prompts on error examples
 **/
public record GradientOptimizerConfig(
        int numberOfGradients,
        int maximumErrorExamples,
        int numberOfErrors,
        int numberOfGradientsPerError,
        int stepsPerGradient,
        int monteCarloSamplesPerStep,
        int maximumExpansionFactor,
        boolean rejectOnErrors,
        int evaluationBudget,
        int minibatchSize,
        int beamSize,
        int candidateOversamplingFactor,
        String gradientPrompt,
        String transformationPrompt,
        String synonymPrompt,
        String feedbackExampleBlock,
        SampleStrategy minibatchSampler,
        SampleStrategy errorSampler,
        SampleStrategy candidateFilterSampler,
        Selector candidateEvaluationSelector,
        Selector errorEvaluationSelector) {

    // Default prompts from the original implementation

    private static final String DEFAULT_GRADIENT_PROMPT = """
                I'm trying to write a zero-shot classifier prompt.

                My current prompt is:
                "%s"

                But this prompt gets the following examples wrong:
                %s

                give %d reasons why the prompt could have gotten these examples wrong.
                Wrap each reason with <START> and <END>
                """;

    private static final String DEFAULT_TRANSFORMATION_PROMPT = """
            I'm trying to write a zero-shot classifier.

            My current prompt is:
            "%s"

            But it gets the following examples wrong:
            %s

            Based on these examples the problem with this prompt is that %s

            Based on the above information, I wrote %d different improved prompts.
            Each prompt is wrapped with <START> and <END>.

            The %d new prompts are:
            """;

    private static final String DEFAULT_SYNONYM_PROMPT =
            "Generate a variation of the following instruction while keeping the semantic meaning.%n%nInput: %s%n%nOutput:";

    private static final String DEFAULT_FEEDBACK_EXAMPLE_BLOCK = """
            Text: "%s"
            Ground Truth: %s
            Classification Result: %s
            """;

    private static final int DEFAULT_NUMBER_OF_GRADIENTS = 4;

    /**
     * The maximum number of misclassified examples used when filtering candidate prompts.
     * <p>
     * Limiting this number prevents expensive re-evaluations on large error sets while still
     * providing enough feedback to guide the selection of better prompts.
     */
    private static final int DEFAULT_MAX_ERROR_EXAMPLES = 16;

    private static final int DEFAULT_NUMBER_OF_ERRORS = 1;
    private static final int DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR = 1;
    private static final int DEFAULT_STEPS_PER_GRADIENT = 1;
    private static final int DEFAULT_MC_SAMPLES_PER_STEP = 2;
    private static final int DEFAULT_MAX_EXPANSION_FACTOR = 8;
    private static final boolean DEFAULT_REJECT_ON_ERRORS = true;
    private static final int DEFAULT_SAMPLES_PER_EVAL = 32;
    private static final int DEFAULT_EVAL_ROUNDS = 8;
    private static final int DEFAULT_EVAL_PROMPTS_PER_ROUND = 8;
    private static final int DEFAULT_MINIBATCH_SIZE = 64;
    private static final int DEFAULT_BEAM_SIZE = 4;
    private static final int DEFAULT_CANDIDATE_OVERSAMPLING_FACTOR = 2;
    private static final int DEFAULT_SEED = 133742243;

    private static final String DEFAULT_SAMPLER = SamplerFactory.SHUFFLED_SAMPLER;
    private static final String DEFAULT_ERROR_SAMPLER = SamplerFactory.ORDERED_SAMPLER;
    private static final String DEFAULT_FILTER_SAMPLER = SamplerFactory.FIRST_SAMPLER;
    private static final String DEFAULT_FILTER_SELECTOR = "simple";

    public GradientOptimizerConfig(ModuleConfiguration configuration, Selector candidateEvaluationSelector) {
        this(
                configuration.argumentAsInt("number_of_gradients", DEFAULT_NUMBER_OF_GRADIENTS),
                configuration.argumentAsInt("max_error_examples", DEFAULT_MAX_ERROR_EXAMPLES),
                configuration.argumentAsInt("number_of_errors", DEFAULT_NUMBER_OF_ERRORS),
                configuration.argumentAsInt("gradients_per_error", DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR),
                configuration.argumentAsInt("steps_per_gradient", DEFAULT_STEPS_PER_GRADIENT),
                configuration.argumentAsInt("mc_samples_per_step", DEFAULT_MC_SAMPLES_PER_STEP),
                configuration.argumentAsInt("max_expansion_factor", DEFAULT_MAX_EXPANSION_FACTOR),
                configuration.argumentAsBoolean("reject_on_errors", DEFAULT_REJECT_ON_ERRORS),
                configuration.argumentAsInt("samples_per_eval", DEFAULT_SAMPLES_PER_EVAL)
                        * configuration.argumentAsInt("eval_rounds", DEFAULT_EVAL_ROUNDS)
                        * configuration.argumentAsInt("eval_prompts_per_round", DEFAULT_EVAL_PROMPTS_PER_ROUND),
                configuration.argumentAsInt("minibatch_size", DEFAULT_MINIBATCH_SIZE),
                configuration.argumentAsInt("beam_size", DEFAULT_BEAM_SIZE),
                configuration.argumentAsInt("candidate_oversampling_factor", DEFAULT_CANDIDATE_OVERSAMPLING_FACTOR),
                configuration.argumentAsString("gradient_prompt", DEFAULT_GRADIENT_PROMPT),
                configuration.argumentAsString("transformation_prompt", DEFAULT_TRANSFORMATION_PROMPT),
                configuration.argumentAsString("synonym_prompt", DEFAULT_SYNONYM_PROMPT),
                configuration.argumentAsString(
                        FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY, DEFAULT_FEEDBACK_EXAMPLE_BLOCK),
                SamplerFactory.createSampler(
                        configuration.argumentAsString(SAMPLER_CONFIGURATION_KEY, DEFAULT_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                SamplerFactory.createSampler(
                        configuration.argumentAsString("error_sampler", DEFAULT_ERROR_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                SamplerFactory.createSampler(
                        configuration.argumentAsString("filter_sampler", DEFAULT_FILTER_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                candidateEvaluationSelector,
                Selector.createSelector(new ModuleConfiguration(
                        configuration.argumentAsString("filter_selector", DEFAULT_FILTER_SELECTOR),
                        java.util.Collections.emptyMap())));
    }
}
