/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import static edu.kit.kastel.sdq.lissa.ratlr.elementstore.ElementStoreOperations.reduceSourceElementStore;
import static edu.kit.kastel.sdq.lissa.ratlr.elementstore.ElementStoreOperations.reduceTargetElementStore;
import static edu.kit.kastel.sdq.lissa.ratlr.promptmetric.MetricUtils.MAXIMUM_SCORE;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizationUtils.getClassificationTasks;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizationUtils.parseTaggedTextFirst;
import static edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer.PromptOptimizationUtils.sanitizePrompt;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.cache.Cache;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;
import edu.kit.kastel.sdq.lissa.ratlr.utils.ChatLanguageModelUtils;

import dev.langchain4j.model.chat.ChatModel;

public class IterativeOptimizer implements PromptOptimizer {

    /**
     * The default threshold for the score to determine when to stop the optimization process early.
     * The score is based on the metric used for evaluation and only computed on the training data.
     * @see Metric Metric interface for its valid range
     */
    private static final double DEFAULT_THRESHOLD_SCORE = 1.0;

    private static final String THRESHOLD_SCORE_CONFIGURATION_KEY = "threshold_score";

    /**
     * The default maximum number of iterations/requests for the optimization process.
     */
    private static final int DEFAULT_MAXIMUM_ITERATIONS = 5;

    private static final String MAXIMUM_ITERATIONS_CONFIGURATION_KEY = "maximum_iterations";

    /**
     * The placeholder used in the optimization prompt to insert the prompt which should be optimized.
     */
    protected static final String ORIGINAL_PROMPT_PLACEHOLDER = "{original_prompt}";
    /**
     * The placeholder used in the optimization prompt to insert the source element type.
     */
    private static final String SOURCE_TYPE_PLACEHOLDER = "{source_type}";
    /**
     * The placeholder used in the optimization prompt to insert the target element type.
     */
    private static final String TARGET_TYPE_PLACEHOLDER = "{target_type}";

    /**
     * Start marker for the prompt in the optimization template.
     */
    private static final String PROMPT_START = "<prompt>";
    /**
     * End marker for the prompt in the optimization template.
     */
    private static final String PROMPT_END = "</prompt>";

    /**
     * The default template for optimization requests.
     * This template presents two artifacts and asks if they are related.
     * Custom optimization prompts can also use the placeholders {@value SOURCE_TYPE_PLACEHOLDER},
     * {@value TARGET_TYPE_PLACEHOLDER} and should use {@value ORIGINAL_PROMPT_PLACEHOLDER}.
     * The optimized prompt should be enclosed between {@value PROMPT_START} and {@value PROMPT_END}.
     */
    private static final String DEFAULT_OPTIMIZATION_TEMPLATE = """
                    Optimize the following prompt to achieve better classification results for traceability link recovery.
                    Traceability links are to be found in the domain of %s to %s.
                    Do not modify the input and output formats specified by the original prompt.
                    Enclose your optimized prompt with %s brackets.
                    The original prompt is provided below:
                    '''%s'''
                    """.formatted(
            SOURCE_TYPE_PLACEHOLDER, TARGET_TYPE_PLACEHOLDER, PROMPT_START + PROMPT_END, ORIGINAL_PROMPT_PLACEHOLDER);

    private static final String OPTIMIZATION_TEMPLATE_CONFIGURATION_KEY = "optimization_template";

    /**
     * The default size of the training data used for optimization.
     * This is the number of elements in the source store.
     */
    private static final int DEFAULT_TRAINING_DATA_SIZE = 3;

    private static final String TRAINING_DATA_SIZE_CONFIGURATION_KEY = "training_data_size";

    /**
     * Key for the original prompt in the configuration.
     * This key is used to retrieve the original prompt from the configuration.
     */
    protected static final String BASE_PROMPT_CONFIGURATION_KEY = "prompt";

    protected static final String FEEDBACK_EXAMPLE_BLOCK_CONFIGURATION_KEY = "feedback_example_block";

    protected static final String SAMPLER_CONFIGURATION_KEY = "sampler";
    protected static final int DEFAULT_SAMPLER_SEED = 42;
    protected static final String SAMPLER_SEED_CONFIGURATION_KEY = "sampler_seed";

    /**
     * Logger for the prompt optimizer.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IterativeOptimizer.class);
    /**
     * The cache used to store and retrieve prompt optimization LLM requests.
     */
    protected final Cache cache;

    /**
     * Provider for the language model used in classification.
     */
    protected final ChatLanguageModelProvider provider;

    /**
     * The language model instance used for classification.
     */
    protected final ChatModel llm;

    private final String template;

    /**
     * The template used for classification requests with domain specific placeholders replaced in the
     * {@link IterativeOptimizer#optimize(SourceElementStore, TargetElementStore)} method.
     */
    protected String formattedTemplate;

    /**
     * The prompt used for optimization.
     * This is the initial prompt that will be optimized iteratively.
     */
    protected final String optimizationPrompt;

    /**
     * The maximum number of iterations for the optimization process.
     * This limits how many times the prompt can be modified and retried.
     */
    protected final int maximumIterations;

    protected final Set<TraceLink> validTraceLinks;
    protected final Metric metric;
    protected final double thresholdScore;
    private final int trainingDataSize;
    /**
     * Creates a new iterative optimizer with the specified configuration.
     *
     * @param configuration The module configuration containing optimizer settings
     * @param goldStandard The set of trace links that represent the gold standard for evaluation
     * @param metric The metric used to score prompt classification
     */
    public IterativeOptimizer(ModuleConfiguration configuration, Set<TraceLink> goldStandard, Metric metric) {
        this(
                configuration,
                goldStandard,
                metric,
                configuration.argumentAsInt(MAXIMUM_ITERATIONS_CONFIGURATION_KEY, DEFAULT_MAXIMUM_ITERATIONS));
    }

    public IterativeOptimizer(
            ModuleConfiguration configuration, Set<TraceLink> goldStandard, Metric metric, int maximumIterations) {
        this.provider = new ChatLanguageModelProvider(configuration);
        this.template =
                configuration.argumentAsString(OPTIMIZATION_TEMPLATE_CONFIGURATION_KEY, DEFAULT_OPTIMIZATION_TEMPLATE);
        configuration.setArgument(MAXIMUM_ITERATIONS_CONFIGURATION_KEY, maximumIterations);
        this.maximumIterations = maximumIterations;
        this.optimizationPrompt = configuration.argumentAsString(BASE_PROMPT_CONFIGURATION_KEY, "");
        this.thresholdScore =
                configuration.argumentAsDouble(THRESHOLD_SCORE_CONFIGURATION_KEY, DEFAULT_THRESHOLD_SCORE);
        this.cache = CacheManager.getDefaultInstance().getCache(this, provider.cacheParameters());
        this.llm = provider.createChatModel();
        this.validTraceLinks = goldStandard;
        this.metric = metric;
        this.trainingDataSize =
                configuration.argumentAsInt(TRAINING_DATA_SIZE_CONFIGURATION_KEY, DEFAULT_TRAINING_DATA_SIZE);
    }

    @Override
    public String[] optimize(SourceElementStore sourceStore, TargetElementStore targetStore) {
        var source = sourceStore.getAllElements(false).getFirst();
        Element target = targetStore.findSimilar(source).getFirst();
        formattedTemplate = template.replace(
                        SOURCE_TYPE_PLACEHOLDER, source.first().getType())
                .replace(TARGET_TYPE_PLACEHOLDER, target.getType());
        SourceElementStore trainingSourceStore = reduceSourceElementStore(sourceStore, trainingDataSize);
        TargetElementStore trainingTargetStore = reduceTargetElementStore(targetStore, trainingSourceStore);
        List<ClassificationTask> examples =
                getClassificationTasks(trainingSourceStore, trainingTargetStore, validTraceLinks);
        return optimizeIntern(examples);
    }

    /**
     * Optimizes the prompt on the source and target stores by iteratively sending requests to the language model.
     * The optimization continues until the prompts score reaches a threshold or the maximum number of iterations is
     * reached.
     *
     * @param examples The classification tasks used to evaluate the prompts performance
     * @return The optimized prompt after the iterative process
     */
    protected String[] optimizeIntern(List<ClassificationTask> examples) {
        double[] promptScores = new double[maximumIterations];
        String[] optimizedPrompts = new String[maximumIterations];
        int i = 0;
        double promptScore;
        String modifiedPrompt = optimizationPrompt;
        do {
            LOGGER.debug("Iteration {}: RequestPrompt = {}", i, modifiedPrompt);
            promptScore = this.metric.getMetric(modifiedPrompt, examples);
            LOGGER.debug("Iteration {}: {} = {}", i, metric.getName(), promptScore);
            promptScores[i] = promptScore;
            modifiedPrompt = cachedSanitizedRequest(generateOptimizationPrompt(modifiedPrompt));
            optimizedPrompts[i] = modifiedPrompt;
            i++;
        } while (i < maximumIterations && promptScore < thresholdScore);
        LOGGER.info("Iterations {}: {} = {}", i, metric.getName(), promptScores);
        return optimizedPrompts;
    }

    /**
     * Sends a cached request to the language model and extracts the sanitized prompt from the response between the
     * {@value PROMPT_START} and {@value PROMPT_END} tags.
     *
     * @param request The request to send to the language model
     * @param iteration The current iteration number for logging
     * @return The optimized prompt extracted from the response
     */
    protected String cachedSanitizedRequest(String request, int iteration) {
        LOGGER.debug("Sending request to LLM (iteration {})...", iteration);
        LOGGER.trace("Full LLM Request:\n{}", request);

        String response = ChatLanguageModelUtils.cachedRequest(request, provider, llm, cache);

        LOGGER.debug("Received response from LLM (iteration {})", iteration);
        LOGGER.trace("Full LLM Response:\n{}", response);

        String sanitized = sanitizePrompt(parseTaggedTextFirst(response, PROMPT_START, PROMPT_END));
        LOGGER.debug("Extracted and sanitized prompt (iteration {})", iteration);
        LOGGER.trace("Extracted Prompt:\n{}", sanitized);

        return sanitized;
    }

    /**
     * Sends a cached request to the language model and extracts the sanitized prompt from the response between the
     * {@value PROMPT_START} and {@value PROMPT_END} tags.
     *
     * @param request The request to send to the language model
     * @return The optimized prompt extracted from the response
     */
    protected String cachedSanitizedRequest(String request) {
        String response = ChatLanguageModelUtils.cachedRequest(request, provider, llm, cache);
        return sanitizePrompt(parseTaggedTextFirst(response, PROMPT_START, PROMPT_END));
    }

    protected String generateOptimizationPrompt(String basePrompt) {
        return formattedTemplate.replace(ORIGINAL_PROMPT_PLACEHOLDER, basePrompt);
    }

    /**
     * Determines if a classification task is classified as a trace link based on the metric score.
     * A task is classified as a trace link if the metric score only deviates from the maximum score by a negligible amount (1e-6).
     *
     * @param prompt The prompt used for classification
     * @param task   The classification task to evaluate
     * @return true if the task is classified as a trace link, false otherwise
     */
    protected boolean isClassifiedCorrectly(String prompt, ClassificationTask task) {
        double taskScore = metric.getMetric(prompt, List.of(task));
        return Math.abs(taskScore - MAXIMUM_SCORE) < 1e-6;
    }
}
