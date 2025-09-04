/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.cli.command;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.sdq.lissa.ratlr.Evaluation;

import picocli.CommandLine;

/**
 * Command implementation for evaluating trace link analysis configurations.
 * This command processes one or more configuration files to run the trace link analysis
 * pipeline and evaluate its results. It supports both single configuration files and
 * directories containing multiple configuration files.
 */
@CommandLine.Command(
        name = "eval",
        mixinStandardHelpOptions = true,
        description = "Invokes the pipeline and evaluates it")
public class EvaluateCommand implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(EvaluateCommand.class);

    /**
     * Array of configuration file paths to be processed.
     * If a path points to a directory, all files within that directory will be processed.
     * If no paths are provided, the command will look for a default "config.json" file.
     */
    @CommandLine.Option(
            names = {"-c", "--configs"},
            arity = "1..*",
            description =
                    "Specifies one or more config paths to be invoked by the pipeline iteratively. If the path points to a directory, all files inside are chosen to get invoked.")
    private Path[] configs;

    /**
     * Whether the intermediate results produced for each evaluation should be saved.
     */
    @CommandLine.Option(
            names = {"-a", "--analyze"},
            description = """
                        Specifies whether intermediate results of each evaluation should be saved. This will contain:
                                - The total element tree of both source and target elements.
                                - A mapping of local cache keys to identify cached classification responses.
                        """
    )
    private boolean saveAnalysis;

    /**
     * Executes the evaluation command.
     * This method:
     * 1. Loads the specified configuration files (or uses default if none specified)
     * 2. Processes each configuration file sequentially
     * 3. Runs the trace link analysis pipeline for each configuration
     * 4. Handles any exceptions that occur during processing
     */
    @Override
    public void run() {
        List<Path> configsToEvaluate = loadConfigs();
        logger.info("Found {} config files to invoke", configsToEvaluate.size());

        for (Path config : configsToEvaluate) {
            logger.info("Invoking the pipeline with '{}'", config);
            try {
                var evaluation = new Evaluation(config, saveAnalysis);
                evaluation.run();
            } catch (Exception e) {
                logger.warn("Configuration '{}' threw an exception: {}", config, e.toString());
            }
        }
    }

    private List<Path> loadConfigs() {
        List<Path> configsToEvaluate = new LinkedList<>();
        if (configs == null) {
            Path defaultConfig = Path.of("config.json");
            if (Files.notExists(defaultConfig)) {
                logger.warn(
                        "Default config '{}' does not exist and no config paths provided, so there is nothing to work with",
                        defaultConfig);
                return List.of();
            }
            configsToEvaluate.add(defaultConfig);
        } else {
            addSpecifiedConfigPaths(configsToEvaluate);
        }

        return configsToEvaluate;
    }

    private void addSpecifiedConfigPaths(List<Path> configsToEvaluate) {
        for (Path configPath : configs) {
            if (Files.notExists(configPath)) {
                logger.warn("Specified config path '{}' does not exist", configPath);
                continue;
            }

            if (!Files.isDirectory(configPath)) {
                configsToEvaluate.add(configPath);
                continue;
            }

            try (DirectoryStream<Path> configDir = Files.newDirectoryStream(configPath)) {
                for (Path configDirEntry : configDir) {
                    if (!Files.isDirectory(configDirEntry)) {
                        configsToEvaluate.add(configDirEntry);
                    }
                }
            } catch (IOException e) {
                logger.warn(
                        "Skipping specified config path '{}' due to causing an exception: {}",
                        configPath,
                        e.getMessage());
            }
        }
    }
}
