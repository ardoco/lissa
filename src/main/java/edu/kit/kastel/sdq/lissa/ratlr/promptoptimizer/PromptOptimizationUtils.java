/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.promptoptimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.SourceElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.elementstore.TargetElementStore;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;
import edu.kit.kastel.sdq.lissa.ratlr.knowledge.TraceLink;
import edu.kit.kastel.sdq.lissa.ratlr.utils.Pair;

public final class PromptOptimizationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptOptimizationUtils.class);
    private static final Pattern REMOVE_LEADING_TRAILING_QUOTES = Pattern.compile("((^[\"']+)|([\"']+$))");

    private PromptOptimizationUtils() {
        throw new IllegalAccessError("Utility class should not be instantiated.");
    }

    /**
     * Parses and extracts a single substring from the input text that is enclosed between the specified start and end tags.
     * If multiple tagged substrings are found, a warning is logged and the first one is returned.
     * If no tagged substrings are found, a warning is logged and the original text is returned.
     *
     * @param text     The input text to parse
     * @param startTag The starting tag to look for
     * @param endTag   The ending tag to look for
     * @return         The extracted substring found between the specified tags, or the original text if none are found
     */
    public static String parseTaggedTextFirst(String text, String startTag, String endTag) {
        List<String> taggedTexts = parseTaggedText(text, startTag, endTag);
        if (taggedTexts.size() > 1) {
            LOGGER.warn("Multiple tagged texts found, using the first one.");
        }
        if (taggedTexts.isEmpty()) {
            LOGGER.warn("No tagged text found, returning the original text.");
        }
        return parseTaggedText(text, startTag, endTag).stream().findFirst().orElse(text);
    }

    /**
     * Parses and extracts all substrings from the input text that are enclosed between the specified start and end tags.
     * The method uses regular expressions to identify and extract the tagged substrings, allowing for multi-line
     * content and case-insensitive matching of the tags.
     *
     * @param text     The input text to parse
     * @param startTag The starting tag to look for
     * @param endTag   The ending tag to look for
     * @return         A possibly empty list of extracted substrings found between the specified tags
     */
    public static List<String> parseTaggedText(String text, String startTag, String endTag) {
        List<String> texts = new ArrayList<>();
        // pattern for as few characters as possible between start and end tag
        Pattern pattern =
                Pattern.compile("%s(.*?)%s".formatted(startTag, endTag), Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            texts.add(matcher.group(1));
        }
        return texts;
    }

    /**
     * Sanitizes the given prompt string by removing leading and trailing quotes and whitespace.
     *
     * @param prompt The prompt string to sanitize
     * @return The prompt string without leading/trailing quotes and whitespace
     */
    public static String sanitizePrompt(String prompt) {
        // Remove leading and trailing quotes and whitespace
        return REMOVE_LEADING_TRAILING_QUOTES
                .matcher(prompt.trim())
                .replaceAll("")
                .trim();
    }

    /**
     * Apply the {@link #sanitizePrompt(String)} method to a list of prompts.
     *
     * @param prompts the list of prompts to sanitize
     * @return        a list of sanitized prompts
     */
    public static List<String> sanitizePrompts(Collection<String> prompts) {
        return prompts.stream().map(PromptOptimizationUtils::sanitizePrompt).toList();
    }

    /**
     * Generates a list of classification tasks based on the provided source and target element stores,
     * and a set of valid trace links. For each source element, it finds similar target elements and creates
     * a classification task for each source-target pair. The task is marked as positive if the pair exists
     * in the set of valid trace links.
     * <br>
     * Note that not all possible source-target pairs are generated, only those where the target is similar to the source.
     * Some actual Traceability Links thus might not be part of the generated tasks.
     *
     * @param sourceStore The store containing source elements.
     * @param targetStore The store containing target elements.
     * @param validTraceLinks A set of valid trace links used to determine positive classification tasks.
     * @return A list of classification tasks generated from the source and target elements.
     */
    public static List<ClassificationTask> getClassificationTasks(
            SourceElementStore sourceStore, TargetElementStore targetStore, Collection<TraceLink> validTraceLinks) {
        List<ClassificationTask> tasks = new ArrayList<>();
        for (Pair<Element, float[]> source : sourceStore.getAllElements(true)) {
            for (Element target : targetStore.findSimilar(source)) {
                tasks.add(new ClassificationTask(
                        source.first(),
                        target,
                        validTraceLinks.contains(
                                TraceLink.of(source.first().getIdentifier(), target.getIdentifier()))));
            }
        }
        return tasks;
    }
}
