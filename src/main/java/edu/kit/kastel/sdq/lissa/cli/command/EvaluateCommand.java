/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.cli.command;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.jspecify.annotations.Nullable;
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
    private static final int MAX_RECURSION_DEPTH = 50;

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
    private Path @Nullable [] configs;

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
        List<Path> configsToEvaluate = loadConfigs(configs);
        logger.info("Found {} config files to invoke", configsToEvaluate.size());

        for (Path config : configsToEvaluate) {
            logger.info("Invoking the pipeline with '{}'", config);
            try {
                var evaluation = new Evaluation(config);
                evaluation.run();
            } catch (Exception e) {
                logger.warn("Configuration '{}' threw an exception: {}", config, e.getMessage());
            }
        }
    }

    /**
     * Loads configuration files based on the provided paths or defaults.
     * If no paths are provided, it attempts to load a default "config.json" file.
     * If paths are provided, it processes each path, adding files or recursively
     * exploring directories to gather all configuration files.
     *
     * @param configs An array of configuration file paths or directories
     * @return A list of configuration file paths to be evaluated
     */
    public static List<Path> loadConfigs(Path[] configs) {
        List<Path> configsToEvaluate = new LinkedList<>();
        if (configs == null) {
            Path defaultConfig = Path.of("config.json");
            if (Files.notExists(defaultConfig)) {
                logger.warn(
                        "Default config '{}' does not exist and no config paths provided, so there is nothing to work with",
                        defaultConfig);
                return List.of();
            }
            return List.of(defaultConfig);
        }
        addSpecifiedConfigPaths(configsToEvaluate, configs);
        return configsToEvaluate;
    }

    /**
     * Adds configuration files from the specified array of paths to the list.
     * Each path is processed to add either the file directly or to explore
     * directories recursively.
     *
     * @param configsToEvaluate The list to which configuration files will be added
     * @param configs An array of configuration file paths or directories
     */
    private static void addSpecifiedConfigPaths(List<Path> configsToEvaluate, Path[] configs) {
        assert configs != null;
        for (Path configPath : configs) {
            addSpecifiedConfigPaths(configsToEvaluate, configPath, 0);
        }
    }

    /**
     * Recursively adds configuration files from the specified path to the list.
     * If the path is a file, it is added directly. If it is a directory,
     * all files within the directory (and its subdirectories) are added.
     *
     * @param configsToEvaluate The list to which configuration files will be added
     * @param configPath The path to a configuration file or directory
     * @param depth Current recursion depth
     */
    private static void addSpecifiedConfigPaths(List<Path> configsToEvaluate, Path configPath, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            logger.warn("Maximum recursion depth exceeded for path '{}', skipping", configPath);
            return;
        }

        if (Files.notExists(configPath)) {
            logger.warn("Specified config path '{}' does not exist", configPath);
            return;
        }

        if (!Files.isDirectory(configPath)) {
            configsToEvaluate.add(configPath);
            return;
        }

        try (DirectoryStream<Path> configDir = Files.newDirectoryStream(configPath)) {
            for (Path configDirEntry : configDir) {
                if (!Files.isDirectory(configDirEntry)) {
                    configsToEvaluate.add(configDirEntry);
                } else {
                    addSpecifiedConfigPaths(configsToEvaluate, configDirEntry, depth + 1);
                }
            }
        } catch (IOException e) {
            logger.warn(
                    "Skipping specified config path '{}' due to causing an exception: {}", configPath, e.getMessage());
        }
    }
}
