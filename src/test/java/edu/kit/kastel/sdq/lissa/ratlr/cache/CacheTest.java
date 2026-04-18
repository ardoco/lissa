/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.kit.kastel.sdq.lissa.ratlr.configuration.ModuleConfiguration;

class CacheTest {
    @TempDir
    private Path tempCacheDir;

    @BeforeEach
    void setup() throws IOException {
        // Reset the default cache manager singleton for each test
        CacheManager.setCacheDir(tempCacheDir.toString());
    }

    @AfterEach
    void teardown() {
        // Clean up the cache manager after each test
        CacheManager.resetDefaultInstance();
    }

    @Test
    void testCacheDirectoryCreatedFromModuleConfiguration() throws IOException {
        CacheManager.resetDefaultInstance();
        Path customCacheDir = tempCacheDir.resolve("custom_cache");
        assertFalse(Files.exists(customCacheDir), "Custom cache directory should not exist before initialization");
        assertThrows(
                IllegalStateException.class,
                CacheManager::getDefaultInstance,
                "Default CacheManager instance should be null before initialization");

        ModuleConfiguration config = new ModuleConfiguration("cache", Map.of("cache_dir", customCacheDir.toString()));
        CacheManager.setCacheDir(config);

        assertTrue(Files.exists(customCacheDir), "Custom cache directory should be created after initialization");
        assertDoesNotThrow(
                CacheManager::getDefaultInstance,
                "Default CacheManager instance should be initialized after setting cache dir");
    }

    @Test
    void testCacheDirectoryCreatedFromCacheDir() throws IOException {
        CacheManager.resetDefaultInstance();
        Path customCacheDir = tempCacheDir.resolve("custom_cache");
        assertFalse(Files.exists(customCacheDir), "Custom cache directory should not exist before initialization");
        assertThrows(
                IllegalStateException.class,
                CacheManager::getDefaultInstance,
                "Default CacheManager instance should be null before initialization");
        CacheManager.setCacheDir(customCacheDir.toString());

        assertTrue(Files.exists(customCacheDir), "Custom cache directory should be created after initialization");
        assertDoesNotThrow(
                CacheManager::getDefaultInstance,
                "Default CacheManager instance should be initialized after setting cache dir");
    }
}
