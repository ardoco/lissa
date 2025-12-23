/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeFeedbackOptimizer.FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.IterativeOptimizer.SAMPLER_CONFIGURATION_KEY;

import java.util.Random;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SampleStrategy;
import edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.samplestrategy.SamplerFactory;

public record GradientOptimizerConfig(
        int numberOfGradients,
        int maxErrorExamples,
        int numberOfErrors,
        int numberOfGradientsPerError,
        int stepsPerGradient,
        int mcSamplesPerStep,
        int maxExpansionFactor,
        boolean rejectOnErrors,
        int evaluationBudget,
        int minibatchSize,
        int beamSize,
        String gradientPrompt,
        String transformationPrompt,
        String synonymPrompt,
        String feedbackExampleBlock,
        SampleStrategy sampleStrategy) {

    // Default prompts from the original implementation

    private static final String GRADIENT_PROMPT_CONFIGURATION_KEY = "gradient_prompt";
    private static final String DEFAULT_GRADIENT_PROMPT =
            """
                I'm trying to write a zero-shot classifier prompt.

                My current prompt is:
                "%s"

                But this prompt gets the following examples wrong:
                %s

                give %d reasons why the prompt could have gotten these examples wrong.
                Wrap each reason with <START> and <END>
                """;

    private static final String TRANSFORMATION_PROMPT_CONFIGURATION_KEY = "transformation_prompt";
    private static final String DEFAULT_TRANSFORMATION_PROMPT =
            """
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

    private static final String SYNONYM_PROMPT_CONFIGURATION_KEY = "synonym_prompt";
    private static final String DEFAULT_SYNONYM_PROMPT =
            "Generate a variation of the following instruction while keeping the semantic meaning.%n%nInput: %s%n%nOutput:";

    private static final String DEFAULT_FEEDBACK_EXAMPLE_BLOCK =
            """
            Text: "%s"
            Ground Truth: %s
            Classification Result: %s
            """;

    private static final String NUMBER_OF_GRADIENTS_CONFIGURATION_KEY = "number_of_gradients";
    private static final int DEFAULT_NUMBER_OF_GRADIENTS = 4;

    /**
     * The maximum number of misclassified examples used when filtering candidate prompts.
     * <p>
     * Limiting this number prevents expensive re-evaluations on large error sets while still
     * providing enough feedback to guide the selection of better prompts.
     */
    private static final int DEFAULT_MAX_ERROR_EXAMPLES = 16;

    private static final String MAX_ERROR_EXAMPLES_CONFIGURATION_KEY = "max_error_examples";

    private static final String NUMBER_OF_ERRORS_CONFIGURATION_KEY = "number_of_errors";
    private static final int DEFAULT_NUMBER_OF_ERRORS = 1;
    private static final String NUMBER_OF_GRADIENTS_PER_ERROR_CONFIGURATION_KEY = "gradients_per_error";
    private static final int DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR = 1;
    private static final String STEPS_PER_GRADIENT_CONFIGURATION_KEY = "steps_per_gradient";
    private static final int STEPS_PER_GRADIENT = 1;
    private static final String MC_SAMPLES_PER_STEP_CONFIGURATION_KEY = "mc_samples_per_step";
    private static final int MC_SAMPLES_PER_STEP = 2;
    private static final String MAX_EXPANSION_FACTOR_CONFIGURATION_KEY = "max_expansion_factor";
    private static final int MAX_EXPANSION_FACTOR = 8;
    private static final String REJECT_ON_ERRORS_CONFIGURATION_KEY = "reject_on_errors";
    private static final boolean REJECT_ON_ERRORS = true;
    private static final String SAMPLES_PER_EVAL_CONFIGURATION_KEY = "samples_per_eval";
    private static final int SAMPLES_PER_EVAL = 32;
    private static final String EVAL_ROUNDS_CONFIGURATION_KEY = "eval_rounds";
    private static final int EVAL_ROUNDS = 8;
    private static final String EVAL_PROMPTS_PER_ROUND_CONFIGURATION_KEY = "eval_prompts_per_round";
    private static final int EVAL_PROMPTS_PER_ROUND = 8;
    private static final String MINIBATCH_SIZE_CONFIGURATION_KEY = "minibatch_size";
    private static final int DEFAULT_MINIBATCH_SIZE = 64;
    private static final String BEAM_SIZE_CONFIGURATION_KEY = "beam_size";
    private static final int BEAM_SIZE = 4;
    private static final String SEED_CONFIGURATION_KEY = "seed";
    private static final int DEFAULT_SEED = 133742243;

    private static final String DEFAULT_SAMPLER = SamplerFactory.SHUFFLED_SAMPLER;

    public GradientOptimizerConfig(ModuleConfiguration configuration) {
        this(
                configuration.argumentAsInt(NUMBER_OF_GRADIENTS_CONFIGURATION_KEY, DEFAULT_NUMBER_OF_GRADIENTS),
                configuration.argumentAsInt(MAX_ERROR_EXAMPLES_CONFIGURATION_KEY, DEFAULT_MAX_ERROR_EXAMPLES),
                configuration.argumentAsInt(NUMBER_OF_ERRORS_CONFIGURATION_KEY, DEFAULT_NUMBER_OF_ERRORS),
                configuration.argumentAsInt(
                        NUMBER_OF_GRADIENTS_PER_ERROR_CONFIGURATION_KEY, DEFAULT_NUMBER_OF_GRADIENTS_PER_ERROR),
                configuration.argumentAsInt(STEPS_PER_GRADIENT_CONFIGURATION_KEY, STEPS_PER_GRADIENT),
                configuration.argumentAsInt(MC_SAMPLES_PER_STEP_CONFIGURATION_KEY, MC_SAMPLES_PER_STEP),
                configuration.argumentAsInt(MAX_EXPANSION_FACTOR_CONFIGURATION_KEY, MAX_EXPANSION_FACTOR),
                configuration.argumentAsBoolean(REJECT_ON_ERRORS_CONFIGURATION_KEY, REJECT_ON_ERRORS),
                configuration.argumentAsInt(SAMPLES_PER_EVAL_CONFIGURATION_KEY, SAMPLES_PER_EVAL)
                        * configuration.argumentAsInt(EVAL_ROUNDS_CONFIGURATION_KEY, EVAL_ROUNDS)
                        * configuration.argumentAsInt(EVAL_PROMPTS_PER_ROUND_CONFIGURATION_KEY, EVAL_PROMPTS_PER_ROUND),
                configuration.argumentAsInt(MINIBATCH_SIZE_CONFIGURATION_KEY, DEFAULT_MINIBATCH_SIZE),
                configuration.argumentAsInt(BEAM_SIZE_CONFIGURATION_KEY, BEAM_SIZE),
                configuration.argumentAsString(GRADIENT_PROMPT_CONFIGURATION_KEY, DEFAULT_GRADIENT_PROMPT),
                configuration.argumentAsString(TRANSFORMATION_PROMPT_CONFIGURATION_KEY, DEFAULT_TRANSFORMATION_PROMPT),
                configuration.argumentAsString(SYNONYM_PROMPT_CONFIGURATION_KEY, DEFAULT_SYNONYM_PROMPT),
                configuration.argumentAsString(
                        FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY, DEFAULT_FEEDBACK_EXAMPLE_BLOCK),
                SamplerFactory.createSampler(
                        configuration.argumentAsString(SAMPLER_CONFIGURATION_KEY, DEFAULT_SAMPLER),
                        new Random(configuration.argumentAsInt(SEED_CONFIGURATION_KEY, DEFAULT_SEED))));
    }
}
