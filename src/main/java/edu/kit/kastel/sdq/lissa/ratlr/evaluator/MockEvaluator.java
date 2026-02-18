/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.evaluator;

import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.lissa.ratlr.classifier.ClassificationTask;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;
import edu.kit.kastel.sdq.lissa.ratlr.promptmetric.Metric;

public class MockEvaluator extends Evaluator {

    public MockEvaluator() {
        super(new ModuleConfiguration("", Collections.emptyMap()));
    }

    @Override
    public List<Double> sampleAndEvaluate(List<String> prompts, List<ClassificationTask> examples, Metric metric) {
        return Collections.nCopies(prompts.size(), 1.0);
    }
}
