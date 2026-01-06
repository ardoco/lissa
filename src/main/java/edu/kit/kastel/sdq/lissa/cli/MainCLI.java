/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.cli;

import java.nio.file.Path;

import edu.kit.kastel.sdq.lissa.cli.command.EvaluateCommand;
import edu.kit.kastel.sdq.lissa.cli.command.OptimizeCommand;
import edu.kit.kastel.sdq.lissa.cli.command.TransitiveTraceCommand;

import picocli.CommandLine;

/**
 * Main command-line interface for the LiSSA framework.
 * This class serves as the entry point for the CLI application and provides
 * subcommands for different functionalities:
 * <ul>
 *     <li>{@link EvaluateCommand} - Evaluates trace link analysis configurations</li>
 *     <li>{@link TransitiveTraceCommand} - Performs transitive trace link analysis</li>
 *     <li>{@link OptimizeCommand} - Optimize a single prompt for better trace link analysis classification results</li>
 * </ul>
 *
 * The CLI supports various command-line options and provides help information
 * through the standard help options (--help, -h).
 */
@CommandLine.Command(subcommands = {EvaluateCommand.class, TransitiveTraceCommand.class, OptimizeCommand.class})
public final class MainCLI {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MainCLI() {}

    /**
     * Main entry point for the CLI application.
     *
     * @param args Command line arguments to be processed
     */
    public static void main(String[] args) {
        new CommandLine(new MainCLI()).registerConverter(Path.class, Path::of).execute(args);
    }
}
