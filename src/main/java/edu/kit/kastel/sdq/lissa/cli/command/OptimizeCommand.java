/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.cli.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import edu.kit.kastel.sdq.lissa.ratlr.configuration.Configuration;
import edu.kit.kastel.sdq.lissa.ratlr.optimizer.PromptOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * CLI subcommand for running LiSSA with a prompt-optimization loop.
 * This command accepts a configuration file, a maximum number of optimization iterations,
 * and a target F1 score. It delegates the optimization process to the PromptOptimizer.
 */
@CommandLine.Command(name = "optimize", description = "Runs LiSSA with a prompt-optimization loop")
public final class OptimizeCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(OptimizeCommand.class);

    @CommandLine.Option(
            names = {"-c", "--config"},
            required = true,
            description = "Path to the LiSSA JSON configuration")
    private Path config;

    @CommandLine.Option(
            names = {"--max-iter"},
            defaultValue = "1",
            description = "Maximum optimization iterations (≥1)")
    private int maxIterations;

    @CommandLine.Option(
            names = {"--target-f1"},
            defaultValue = "0.40",
            description = "Target F1 score to reach before stopping")
    private double targetF1;

    /**
     * Executes the optimization process with the provided configuration.
     * This method initializes the PromptOptimizer and starts the optimization loop.
     */
    @Override
    public void run() {
        logger.info("Starting optimization for {}", config);
        try {
            Configuration fullConfig = new ObjectMapper().readValue(config.toFile(), Configuration.class);
            String cacheDirectoryString = fullConfig.cacheDir();
            if (cacheDirectoryString != null) {
                CacheManager.setCacheDir(cacheDirectoryString);
                logger.info("Cache directory set to {}", cacheDirectoryString);
            } else {
                logger.warn("No cache directory found in configuration, caching may be disabled.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try {
            new PromptOptimizer(config, maxIterations, targetF1).optimize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
