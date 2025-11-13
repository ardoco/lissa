/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.cli;

import edu.kit.kastel.sdq.lissa.cli.command.EvaluateCommand;
import edu.kit.kastel.sdq.lissa.cli.command.OptimizeCommand;
import edu.kit.kastel.sdq.lissa.cli.command.TransitiveTraceCommand;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Arrays;

/**
 * Main command-line interface for the LiSSA framework.
 * This class serves as the entry point for the CLI application and provides
 * subcommands for different functionalities:
 * <ul>
 *     <li>{@link EvaluateCommand} - Evaluates trace link analysis configurations</li>
 *     <li>{@link TransitiveTraceCommand} - Performs transitive trace link analysis</li>
 * </ul>
 * <p>
 * The CLI supports various command-line options and provides help information
 * through the standard help options (--help, -h).
 */
@CommandLine.Command(subcommands = {EvaluateCommand.class, TransitiveTraceCommand.class, OptimizeCommand.class})
public final class MainCLI {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MainCLI() {
    }

    /**
     * Main entry point for the CLI application.
     *
     * @param args Command line arguments to be processed
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{
                    "optimize",
                    "-c", "configs\\req2req\\CM1-NASA_simple_gpt_gpt-4o-mini-2024-07-18.json",
                    "--max-iter", "3",
                    "--target-f1", "0.40"
            };
        }
        System.out.println("ARGS: " + Arrays.toString(args));
        new CommandLine(new MainCLI()).registerConverter(Path.class, Path::of).execute(args);
    }
}
