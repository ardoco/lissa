/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeFeedbackOptimizer.FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeOptimizer.SAMPLER_CONFIGURATION_KEY;

import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.promptselector.Selector;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy;

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
public record ProTeGiOptimizerConfig(
        int numberOfGradients,
        int maximumErrorExamples,
        int numberOfErrors,
        int numberOfGradientsPerError,
        int stepsPerGradient,
        int monteCarloSamplesPerStep,
        int maximumExpansionFactor,
        boolean rejectOnErrors,
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

    /**
     * Compact constructor that validates all configuration parameters to ensure the optimizer operates correctly.
     * <p>
     * Validation guarantees:
     * <ul>
     *     <li>beamSize &gt;= 1: Required to ensure at least one candidate prompt is selected after scoring/filtering</li>
     *     <li>numberOfGradients &gt;= 1: Required to generate at least one gradient for prompt improvement</li>
     *     <li>numberOfErrors &gt;= 1: Required to sample at least one error for gradient generation</li>
     *     <li>numberOfGradientsPerError &gt;= 1: Required to generate gradient feedback for each error</li>
     *     <li>stepsPerGradient &gt;= 1: Required to generate at least one prompt variation per gradient</li>
     *     <li>monteCarloSamplesPerStep &gt;= 1: Required to generate at least one Monte Carlo sample per step</li>
     *     <li>maximumExpansionFactor &gt;= 1: Required to ensure at least one candidate prompt in expansion</li>
     *     <li>minibatchSize &gt;= 1: Required to ensure at least one task per minibatch during sampling</li>
     *     <li>maximumErrorExamples &gt;= 1: Required to ensure at least one error example for filtering</li>
     *     <li>candidateOversamplingFactor &gt;= 1: Required to ensure positive oversampling during candidate filtering</li>
     *     <li>Prompt templates must not be empty</li>
     *
     * </ul>
     *
     * @throws IllegalArgumentException if any configuration value violates the above constraints
     */
    public ProTeGiOptimizerConfig {
        if (beamSize < 1) {
            throw new IllegalArgumentException("Invalid configuration: beamSize must be >= 1, but got " + beamSize
                    + ". This parameter ensures at least one candidate prompt is selected after scoring/filtering.");
        }
        if (numberOfGradients < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: numberOfGradients must be >= 1, but got " + numberOfGradients
                            + ". This parameter ensures at least one gradient is generated for prompt improvement.");
        }
        if (numberOfErrors < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: numberOfErrors must be >= 1, but got " + numberOfErrors
                            + ". This parameter ensures at least one error is sampled for gradient generation.");
        }
        if (numberOfGradientsPerError < 1) {
            throw new IllegalArgumentException("Invalid configuration: numberOfGradientsPerError must be >= 1, but got "
                    + numberOfGradientsPerError
                    + ". This parameter ensures gradient feedback is generated for each error.");
        }
        if (stepsPerGradient < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: stepsPerGradient must be >= 1, but got " + stepsPerGradient
                            + ". This parameter ensures at least one prompt variation is generated per gradient.");
        }
        if (monteCarloSamplesPerStep < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: monteCarloSamplesPerStep must be >= 1, but got " + monteCarloSamplesPerStep
                            + ". This parameter ensures at least one Monte Carlo sample is generated per step.");
        }
        if (maximumExpansionFactor < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: maximumExpansionFactor must be >= 1, but got " + maximumExpansionFactor
                            + ". This parameter ensures at least one candidate prompt is generated in expansion.");
        }
        if (minibatchSize < 1) {
            throw new IllegalArgumentException("Invalid configuration: minibatchSize must be >= 1, but got "
                    + minibatchSize + ". This parameter ensures at least one task per minibatch during sampling.");
        }
        if (maximumErrorExamples < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: maximumErrorExamples must be >= 1, but got " + maximumErrorExamples
                            + ". This parameter ensures at least one error example is considered when filtering.");
        }
        if (candidateOversamplingFactor < 1) {
            throw new IllegalArgumentException(
                    "Invalid configuration: candidateOversamplingFactor must be >= 1, but got "
                            + candidateOversamplingFactor
                            + ". This parameter ensures positive oversampling during candidate filtering.");
        }
        if (gradientPrompt.isBlank()) {
            throw new IllegalArgumentException("Invalid configuration: gradientPrompt must not be null or empty");
        }
        if (transformationPrompt.isBlank()) {
            throw new IllegalArgumentException("Invalid configuration: transformationPrompt must not be null or empty");
        }
        if (synonymPrompt.isBlank()) {
            throw new IllegalArgumentException("Invalid configuration: synonymPrompt must not be null or empty");
        }
        if (feedbackExampleBlock.isBlank()) {
            throw new IllegalArgumentException("Invalid configuration: feedbackExampleBlock must not be null or empty");
        }
    }

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
    private static final int DEFAULT_MINIBATCH_SIZE = 64;
    private static final int DEFAULT_BEAM_SIZE = 4;
    private static final int DEFAULT_CANDIDATE_OVERSAMPLING_FACTOR = 2;
    private static final int DEFAULT_SEED = 133742243;

    private static final String DEFAULT_SAMPLER = SampleStrategy.SHUFFLED_SAMPLER;
    private static final String DEFAULT_ERROR_SAMPLER = SampleStrategy.ORDERED_SAMPLER;
    private static final String DEFAULT_FILTER_SAMPLER = SampleStrategy.FIRST_SAMPLER;
    private static final String DEFAULT_FILTER_SELECTOR = "simple";

    public ProTeGiOptimizerConfig(ModuleConfiguration configuration, Selector candidateEvaluationSelector) {
        this(
                configuration.argumentAsInt("number_of_gradients", DEFAULT_NUMBER_OF_GRADIENTS),
                configuration.argumentAsInt("max_error_examples", DEFAULT_MAX_ERROR_EXAMPLES),
                configuration.argumentAsInt("number_of_errors", DEFAULT_NUMBER_OF_ERRORS),
                configuration.argumentAsInt("gradients_per_error", DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR),
                configuration.argumentAsInt("steps_per_gradient", DEFAULT_STEPS_PER_GRADIENT),
                configuration.argumentAsInt("mc_samples_per_step", DEFAULT_MC_SAMPLES_PER_STEP),
                configuration.argumentAsInt("max_expansion_factor", DEFAULT_MAX_EXPANSION_FACTOR),
                configuration.argumentAsBoolean("reject_on_errors", DEFAULT_REJECT_ON_ERRORS),
                configuration.argumentAsInt("minibatch_size", DEFAULT_MINIBATCH_SIZE),
                configuration.argumentAsInt("beam_size", DEFAULT_BEAM_SIZE),
                configuration.argumentAsInt("candidate_oversampling_factor", DEFAULT_CANDIDATE_OVERSAMPLING_FACTOR),
                configuration.argumentAsString("gradient_prompt", DEFAULT_GRADIENT_PROMPT),
                configuration.argumentAsString("transformation_prompt", DEFAULT_TRANSFORMATION_PROMPT),
                configuration.argumentAsString("synonym_prompt", DEFAULT_SYNONYM_PROMPT),
                configuration.argumentAsString(
                        FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY, DEFAULT_FEEDBACK_EXAMPLE_BLOCK),
                SampleStrategy.createSampler(
                        configuration.argumentAsString(SAMPLER_CONFIGURATION_KEY, DEFAULT_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                SampleStrategy.createSampler(
                        configuration.argumentAsString("error_sampler", DEFAULT_ERROR_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                SampleStrategy.createSampler(
                        configuration.argumentAsString("filter_sampler", DEFAULT_FILTER_SAMPLER),
                        new Random(configuration.argumentAsInt("seed", DEFAULT_SEED))),
                candidateEvaluationSelector,
                Selector.createSelector(new ModuleConfiguration(
                        configuration.argumentAsString("filter_selector", DEFAULT_FILTER_SELECTOR),
                        java.util.Collections.emptyMap())));
    }
}
