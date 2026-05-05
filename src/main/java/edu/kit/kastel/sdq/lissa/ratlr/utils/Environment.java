/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * A utility class for managing environment variables in the application.
 * This class provides functionality to:
 * <ul>
 *     <li>Load environment variables from a .env file</li>
 *     <li>Fall back to system environment variables if .env is not available</li>
 *     <li>Retrieve environment variables with or without null checks</li>
 * </ul>
 *
 * The class uses the following precedence for environment variables:
 * <ol>
 *     <li>Values from the .env file (if it exists)</li>
 *     <li>Values from system environment variables</li>
 * </ol>
 *
 * The .env file should be placed in the root directory of the project and should
 * contain key-value pairs in the format:
 * <pre>
 * KEY=value
 * </pre>
 */
public final class Environment {
    private static final Logger logger = LoggerFactory.getLogger(Environment.class);
    /** The loaded .env configuration, or null if no .env file exists */
    private static @Nullable Dotenv dotenv = load();

    private Environment() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Retrieves an environment variable value.
     * This method:
     * <ol>
     *     <li>First checks the .env file for the variable</li>
     *     <li>If not found, falls back to system environment variables</li>
     *     <li>Returns null if the variable is not found in either location</li>
     * </ol>
     *
     * @param key The name of the environment variable to retrieve
     * @return The value of the environment variable, or null if not found
     */
    public static @Nullable String getenv(String key) {
        String dotenvValue = dotenv == null ? null : dotenv.get(key);
        if (dotenvValue != null) return dotenvValue;
        return System.getenv(key);
    }

    /**
     * Retrieves an environment variable value, requiring it to be non-null.
     * This method:
     * <ol>
     *     <li>Attempts to retrieve the variable using {@link #getenv(String)}</li>
     *     <li>Throws an IllegalStateException if environment variable would be null</li>
     * </ol>
     *
     * @param key The name of the environment variable to retrieve
     * @return The value of the environment variable
     * @throws IllegalStateException if the variable is not found and strict mode is enabled
     */
    public static String getenvNonNull(String key) {
        String env = getenv(key);
        if (env == null) {
            throw new IllegalStateException(
                    "environment variable %s is missing, use '.env' or your system to set it up".formatted(key));
        }
        return env;
    }

    /**
     * Loads the .env file configuration.
     * This method:
     * <ol>
     *     <li>Checks if a .env file exists in the project root</li>
     *     <li>If found, loads and returns the configuration</li>
     *     <li>If not found, logs a message and returns null</li>
     * </ol>
     *
     * The method is synchronized to ensure thread safety during the initial loading.
     *
     * @return The loaded Dotenv configuration, or null if no .env file exists
     */
    private static synchronized @Nullable Dotenv load() {
        if (dotenv != null) {
            return dotenv;
        }

        if (Files.exists(Path.of(".env"))) {
            return Dotenv.configure().load();
        } else {
            logger.info("No .env file found, using system environment variables");
            return null;
        }
    }

    /**
     * Overwrites the current .env configuration with a new one from the specified path.
     * This method:
     * <ol>
     *     <li>Checks if a .env file exists at the given path</li>
     *     <li>If found, loads and sets the new configuration</li>
     *     <li>If not found, logs a warning and retains the existing configuration</li>
     * </ol>
     *
     * The method is synchronized to ensure thread safety when updating the configuration.
     *
     * @param path The path to the new .env file
     */
    public static synchronized void overwrite(Path path) {
        if (Files.exists(path)) {
            dotenv = Dotenv.configure().filename(path.toString()).load();
        } else {
            logger.warn("No .env file found at '{}', using system environment variables", path);
        }
    }
}
