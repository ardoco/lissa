/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Utility class for writing log messages to files in the {@code logs} directory.
 * Ensures that the log directory exists and provides options for appending to or overwriting existing files.
 */
public class LogWriter {

    private static final String LOG_DIR = "logs";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private LogWriter() {
    }


    /**
     * Writes the given content to a log file in the logs directory.
     *
     * @param fileName the name of the log file
     * @param content  the content to write
     * @param append   if true, appends to the file; otherwise overwrites
     */
    public static void write(String fileName, String content, boolean append) throws IOException {
        Files.createDirectories(Path.of(LOG_DIR));
        Path file = Path.of(LOG_DIR, fileName);

        if (append) {
            Files.writeString(file, content + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.writeString(file, content + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}

