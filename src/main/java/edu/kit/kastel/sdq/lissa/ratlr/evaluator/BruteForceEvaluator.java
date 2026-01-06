/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;

/**
 * An evaluator that performs a brute-force evaluation of all provided prompts
 */
public class BruteForceEvaluator extends Evaluator {

    /**
     * Creates a new brute-force evaluator instance with the given configuration.
     *
     * @param configuration The configuration for the evaluator.
     */
    public BruteForceEvaluator(ModuleConfiguration configuration) {
        super(configuration);
    }

    /**
     * Evaluates all provided prompts using the given classifier and metric.
     * It selects a subset of examples based on the evaluation budget and the number of prompts.
     * Each prompt is evaluated against all remaining examples, and the mean score amongst them is returned
     */
    @Override
    public List<Double> sampleAndEvaluate(
            List<String> prompts, List<ClassificationTask> classificationTasks, Metric metric) {
        int sampleSize = Math.min(classificationTasks.size(), (this.evaluationBudget / prompts.size()));
        List<ClassificationTask> classificationExamples = new ArrayList<>(classificationTasks);
        Collections.shuffle(classificationExamples, this.random);
        classificationExamples = classificationExamples.subList(0, sampleSize);
        return metric.getMetric(prompts, classificationExamples);
    }
}
