/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.cli.command;

import static edu.kit.kastel.sdq.lissa.cli.command.EvaluateCommand.loadConfigs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;
import edu.kit.kastel.sdq.lissa.ratlr.Optimization;

import picocli.CommandLine;

/**
 * Command implementation for optimizing prompts used in trace link analysis configurations.
 * This command processes one or more optimization configuration files to run the prompt
 * optimization pipeline, and optionally evaluates the optimized prompts using specified
 * evaluation configuration files.
 */
@CommandLine.Command(
        name = "optimize",
        mixinStandardHelpOptions = true,
        description = "Optimizes a prompt for usage in the pipeline")
public class OptimizeCommand implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizeCommand.class);

    /**
     * Array of optimization configuration file paths to be processed.
     * If a path points to a directory, all files within that directory will be processed.
     * This option is required to run the optimization command.
     */
    @CommandLine.Option(
            names = {"-c", "--configs"},
            arity = "1..*",
            description =
                    "Specifies one or more config paths to be invoked by the pipeline iteratively. If the path points "
                            + "to a directory, all files inside are chosen to get invoked.")
    private Path[] optimizationConfigs;

    /**
     * Array of evaluation configuration file paths to be processed.
     * If a path points to a directory, all files within that directory will be processed.
     * This option is optional; if not provided, no evaluation will be performed after optimization.
     */
    @CommandLine.Option(
            names = {"-e", "--eval"},
            arity = "0..*",
            description = "Specifies optional evaluation config paths to be invoked by the pipeline iteratively. "
                    + "Each evaluation configuration will be used with each optimization config."
                    + "If the path points to a directory, all files inside are chosen to get invoked.")
    private Path[] evaluationConfigs;

    /**
     * Runs the optimization and evaluation pipelines based on the provided configuration files.
     * It first loads the optimization and evaluation configurations, then executes the evaluation
     * pipeline for each evaluation configuration. This is the unoptimized baseline evaluation. <br>
     * After that, it runs the optimization pipeline for
     * each optimization configuration, and subsequently evaluates the optimized prompt using each
     * evaluation configuration once more with the optimized prompt instead of the original one.
     */
    @Override
    public void run() {
        List<Path> configsToOptimize = loadConfigs(optimizationConfigs);
        List<Path> configsToEvaluate = loadConfigs(evaluationConfigs);
        LOGGER.info(
                "Found {} optimization config files and {} evaluation config files to invoke",
                configsToOptimize.size(),
                configsToEvaluate.size());

        for (Path evaluationConfig : configsToEvaluate) {
            runEvaluation(evaluationConfig, "");
        }

        for (Path optimizationConfig : configsToOptimize) {
            LOGGER.info("Invoking the optimization pipeline with '{}'", optimizationConfig);
            String optimizedPrompt = "";
            try {
                var optimization = new Optimization(optimizationConfig);
                optimizedPrompt = optimization.run();
            } catch (IOException e) {
                LOGGER.warn(
                        "Optimization configuration '{}' threw an exception: {} \n Maybe the file does not exist?",
                        optimizationConfig,
                        e.getMessage());
            }
            for (Path evaluationConfig : configsToEvaluate) {
                runEvaluation(evaluationConfig, optimizedPrompt);
            }
        }
    }

    private static void runEvaluation(Path evaluationConfig, String optimizedPrompt) {
        LOGGER.info("Invoking the evaluation pipeline with '{}'", evaluationConfig);
        try {
            var evaluation = new Evaluation(evaluationConfig, optimizedPrompt);
            evaluation.run();
        } catch (IOException e) {
            LOGGER.warn(
                    "Baseline evaluation configuration '{}' threw an exception: {} \n Maybe the file does not exist?",
                    evaluationConfig,
                    e.getMessage());
        }
    }
}
