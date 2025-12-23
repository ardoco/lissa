/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.promptmetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationResult;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.classifier.Classifier;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.reductor.Reductor;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.reductor.ReductorFactory;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.scorer.Scorer;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.scorer.ScorerFactory;

/**
 * A pointwise metric that evaluates each classification task individually with a {@link Scorer} and then aggregates the
 * scores using a {@link Reductor}.
 * It uses a caching mechanism to avoid redundant computations for the same task and prompt combination.
 */
public class PointwiseMetric implements Metric {

    private static final String DEFAULT_SCORER = "binary";
    private static final String SCORER_CONFIGURATION_KEY = "metric";
    private static final String DEFAULT_REDUCTOR = "mean";
    private static final String REDUCTOR_CONFIGURATION_KEY = "reductor";

    private final Scorer scorer;
    private final Reductor reductor;
    private final Classifier classifier;

    public PointwiseMetric(ModuleConfiguration configuration, Classifier classifier) {
        this.scorer =
                ScorerFactory.createScorer(configuration.argumentAsString(SCORER_CONFIGURATION_KEY, DEFAULT_SCORER));
        this.reductor = ReductorFactory.createReductor(
                configuration.argumentAsString(REDUCTOR_CONFIGURATION_KEY, DEFAULT_REDUCTOR));
        this.classifier = classifier;
    }

    /**
     * This method computes scores for a list of prompts and classification task examples.
     * Each prompt is delegated to the {@link #getMetric(String, List)} method for scoring with the entire example set.
     */
    @Override
    public List<Double> getMetric(List<String> prompts, List<ClassificationTask> examples) {
        List<Double> scores = new ArrayList<>();
        for (String prompt : prompts) {
            scores.add(getMetric(prompt, examples));
        }
        return scores;
    }

    /**
     * This method computes the metric for a single prompt against a list of classification tasks.
     * It checks the cache for previously computed scores to avoid redundant computations.
     * If a score is not found in the cache, it classifies the examples and computes the scores using the {@link Scorer}.
     * Finally, it aggregates the scores using the {@link Reductor} and returns the final metric value.
     */
    @Override
    public Double getMetric(String prompt, List<ClassificationTask> examples) {
        List<Double> scores = new ArrayList<>();
        List<ClassificationTask> examplesToCompute = new ArrayList<>(examples);
        List<ClassificationResult> classifications = classify(prompt, examplesToCompute);
        List<Double> computedScores = scorer.score(examplesToCompute, classifications);
        for (int i = 0; i < examplesToCompute.size(); i++) {
            ClassificationTask example = examplesToCompute.get(i);
            scores.add(computedScores.get(i));
        }
        return reductor.reduce(scores);
    }

    @Override
    public String getName() {
        return "%s %s".formatted(scorer.getName(), reductor.getName());
    }

    /**
     * Classifies the given examples using the specified prompt.
     * The results are returned in the same order as the input examples.
     * If no classification result is found for an example, a null value will be returned at the respective position.
     */
    private List<ClassificationResult> classify(String prompt, List<ClassificationTask> examples) {
        List<ClassificationResult> results = new ArrayList<>();
        classifier.setClassificationPrompt(prompt);
        List<ClassificationResult> classifications = classifier.classify(examples);

        Map<String, ClassificationResult> classificationMap = new HashMap<>();
        for (ClassificationResult classification : classifications) {
            classificationMap.put(getClassificationKey(classification), classification);
        }
        for (ClassificationTask task : examples) {
            results.add(classificationMap.get(getClassificationKey(task)));
        }

        return results;
    }

    @NotNull
    private static String getClassificationKey(ClassificationTask task) {
        return getClassificationKey(task.source().toString(), task.target().toString());
    }

    @NotNull
    private static String getClassificationKey(ClassificationResult result) {
        return getClassificationKey(result.source().toString(), result.target().toString());
    }

    @NotNull
    private static String getClassificationKey(String first, String second) {
        return "%s-%s".formatted(first, second);
    }
}
