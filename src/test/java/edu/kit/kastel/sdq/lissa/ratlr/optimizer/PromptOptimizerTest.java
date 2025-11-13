package edu.kit.kastel.sdq.lissa.ratlr.optimizer;

import edu.kit.kastel.sdq.lissa.ratlr.cache.CacheManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the PromptOptimizer.
 * These tests validate that the optimizer can be created and executed without errors,
 * and that it successfully improves the F1 score over the baseline.
 * The tests use a sample configuration file from src/test/resources and rely on
 * logs being written to logs/f1_log.txt. The baseline F1 score of LiSSA is known
 * to be approximately 0.439. Therefore, the optimizer is expected to reach at least
 * 0.44 after optimization.
 */
class PromptOptimizerTest {

    /**
     * Path to the base configuration file used for initializing the optimizer.
     */
    Path baseConfigPath = Path.of("src/test/resources/CM1-NASA_simple_gpt_gpt-4o-mini-2024-07-18.json");

    /**
     * Path to the log file where F1 scores are written during optimization.
     */
    Path logFilePath = Path.of("logs/f1_log.txt");

    /**
     * Initializes the cache directory before running any tests.
     *
     * @throws IOException if setting the cache directory fails
     */
    @BeforeAll
    static void beforeAll() throws IOException {
        CacheManager.setCacheDir(CacheManager.DEFAULT_CACHE_DIRECTORY);
    }

    /**
     * Ensures that creating a new PromptOptimizer instance
     * with a valid configuration does not throw any exceptions.
     */
    @Test
    void creationDoesNotThrow() {
        assertDoesNotThrow(() -> new PromptOptimizer(baseConfigPath, 1, 0.5));
    }

    /**
     * Runs the optimizer and checks whether the achieved F1 score
     * is greater than or equal to the defined improvement threshold (0.44).
     * The last recorded F1 score is read from logs/f1_log.txt, where decimal
     * separators are normalized (comma replaced with dot) before parsing.
     *
     * @throws IOException if reading the log file fails
     */
    @Test
    void optimize() throws IOException {
        // LiSSA's base F1 = 0.439..., so any result above 0.44 indicates improvement
        double f1ImprovementThreshold = 0.44;
        var optimizer = new PromptOptimizer(baseConfigPath, 10, f1ImprovementThreshold);
        optimizer.optimize();

        String lastF1String = Files.readAllLines(logFilePath)
                .stream()
                .filter(line -> line.startsWith("Best F1: "))
                .toList().getLast();
        double lastF1Score = Double.parseDouble(
                lastF1String.substring("Best F1: ".length()).replace(',', '.')
        );

        assertTrue(lastF1Score >= f1ImprovementThreshold);
    }
}
