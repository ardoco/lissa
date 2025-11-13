/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ChatLanguageModelProvider;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Responsible for generating improved classification prompts using an LLM.
 * The PromptWriter builds a meta-prompt that is sent to an LLM.
 * The LLM then produces a refined prompt for LiSSA to use in subsequent iterations.
 */
public final class PromptWriter {

    private final ChatModel llm;
    private final Path resultsFile = Paths.get("logs", "classification_results.json");

    /**
     * Constructs a PromptWriter with the given LLM module configuration.
     * Ensures that a valid OpenAI API key is provided.
     *
     * @param moduleConfig configuration of the LLM module
     */
    public PromptWriter(ModuleConfiguration moduleConfig) {

        String apiKey = Environment.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Missing OPENAI_API_KEY environment variable");
        }

        SortedMap<String, String> args = new TreeMap<>(moduleConfig.arguments());
        if (!args.containsKey("model")
                || args.get("model") == null
                || args.get("model").isBlank()) {
            args.put("model", "gpt-4o-mini-2024-07-18");
        }
        args.put("api_key", apiKey);

        ModuleConfiguration llmModule = new ModuleConfiguration(moduleConfig.name(), args);

        this.llm = new ChatLanguageModelProvider(llmModule).createChatModel();
        CacheManager.getDefaultInstance().getCache("prompt-writer");
    }

    /**
     * Generates an improved classification prompt based on the current template,
     * the last achieved F1 score, and examples of classification results.
     *
     * @param currentTemplate the current classification prompt template
     * @param iteration       current iteration index (1-based)
     * @param lastF1          last achieved F1 score
     * @return PromptGenerationResult containing both the AI meta-prompt and the final prompt for LiSSA
     */
    public PromptGenerationResult improve(String currentTemplate, int iteration, double lastF1) {
        ClassificationResultsManager classificationResultsManager = new ClassificationResultsManager(resultsFile);
        StringBuilder aiPrompt = new StringBuilder();

        // Build the prompt for the LLM which in turn will generate a prompt for LiSSA
        aiPrompt.append(
                """
                You are optimizing a classification task.
                A model called "LiSSA" will be given a high-level requirement and a low-level requirement.\
                "LiSSA" must decide whether these requirements are related or not. \
                "LiSSA" must answer with "yes" or "no".\\

                """);

        if (iteration > 1) {
            aiPrompt.append("The F1 score achieved in the last iteration was: ")
                    .append(String.format("%.4f", lastF1))
                    .append("\n\n");
        } else {
            aiPrompt.append("This is the first iteration, no previous F1 score available.\n\n");
        }

        SortedMap<String, List<String>> examples = classificationResultsManager.loadExamples();
        List<String> selected = classificationResultsManager.pickWeightedExamples(examples);

        aiPrompt.append("The current template is:\n");
        aiPrompt.append(currentTemplate).append("\n\n");
        if (iteration > 1) {
            aiPrompt.append(
                    """
                    Analyze the following examples \
                    ("TP" means "True Positive", "FP" means "False Positive" and \
                    "FN" means "False Negative"):

                    """);

            for (String example : selected) {
                aiPrompt.append(example).append("\n\n");
            }
        } else {
            aiPrompt.append("This is the first iteration, no previous examples.\n\n");
        }
        aiPrompt.append(
                """
                Your task is to write a new classification prompt for LiSSA \
                that reduces false positives and false negatives and thus increases the F1-score \
                in the next iteration.\
                From the examples, extract general rules \
                and incorporate them into the new prompt so that LiSSA classifies better.\
                Your output must ONLY be the final prompt text, no explanations or notes. \
                If you refer to LiSSA directly, call it "you" since it does not know its name.\\
                The Prompt for LiSSA must include placeholders, like in this example prompt:
                "Question: Here are two parts of software development artifacts.

                    {source_type}: '''{source_content}'''

                    {target_type}: '''{target_content}'''
                    Are they related?

                    Answer with 'yes' or 'no'."
                """);

        // Prompt for LiSSA
        String lissaPrompt = llm.chat(aiPrompt.toString());
        return new PromptGenerationResult(aiPrompt.toString(), lissaPrompt);
    }
}
