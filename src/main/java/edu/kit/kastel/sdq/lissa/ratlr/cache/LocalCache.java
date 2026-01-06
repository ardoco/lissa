/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements a local file-based cache for storing key-value pairs.
 * This class provides a thread-safe implementation of a cache that persists its contents
 * to a JSON file. It includes automatic flushing of changes when a certain threshold
 * of modifications is reached.
 */
class LocalCache {
    private final ObjectMapper mapper;

    /**
     * Maximum number of modifications before automatic flush.
     */
    private static final int MAX_DIRTY = 50;

    /**
     * Counter for unflushed modifications.
     */
    private int dirty = 0;

    private final File cacheFile;

    /**
     * In-memory cache storage.
     */
    private Map<String, String> cache = new HashMap<>();

    /**
     * Creates a new local cache instance.
     * The cache will be initialized from the specified file if it exists,
     * or a new file will be created.
     *
     * @param cacheFile The path to the cache file
     */
    LocalCache(String cacheFile) {
        this.cacheFile = new File(cacheFile);
        mapper = new ObjectMapper();
        createLocalStore();
    }

    /**
     * Checks if the cache is ready for use.
     * This method ensures that the cache file exists and is accessible.
     *
     * @return true if the cache is ready, false otherwise
     * @throws UncheckedIOException If there are issues accessing the cache file
     */
    public boolean isReady() {
        try {
            return cacheFile.exists() || cacheFile.createNewFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Initializes the local cache store.
     * If the cache file exists and is not empty, its contents are loaded into memory.
     * If the file is empty, it is deleted to ensure a clean state.
     *
     * @throws IllegalArgumentException If the cache file cannot be read
     */
    private void createLocalStore() {
        if (cacheFile.exists()) {
            try {
                if (Files.readString(cacheFile.toPath()).isBlank()) {
                    cacheFile.delete();
                } else {
                    cache = mapper.readValue(cacheFile, new TypeReference<>() {});
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read cache file (" + cacheFile.getName() + ")", e);
            }
        }
    }

    /**
     * Writes the current cache contents to disk.
     * This method uses a temporary file to ensure atomic writes and prevent data corruption.
     * The dirty counter is reset after a successful write.
     *
     * @throws IllegalArgumentException If the cache file cannot be written
     */
    public synchronized void write() {
        if (dirty == 0) {
            return;
        }

        try {
            File tempFile = new File(cacheFile.getAbsolutePath() + ".tmp.json");
            mapper.writeValue(tempFile, cache);
            Files.copy(tempFile.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(tempFile.toPath());
            dirty = 0;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not write cache file", e);
        }
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key The cache key to look up
     * @return The cached value, or null if not found
     */
    public synchronized @Nullable String get(CacheKey key) {
        return cache.get(key.localKey());
    }

    /**
     * Stores a value in the cache.
     * If the value is different from the existing value (if any), the dirty counter is incremented.
     * If the dirty counter exceeds the maximum threshold, the cache is automatically flushed to disk.
     *
     * @param key The cache key to store the value under
     * @param value The value to store
     */
    public synchronized void put(CacheKey key, String value) {
        String old = cache.put(key.localKey(), value);
        if (old == null || !old.equals(value)) {
            dirty++;
        }

        if (dirty > MAX_DIRTY) {
            write();
        }
    }

    /**
     * Returns true if and only if this map contains a mapping for a key
     *
     * @param key The cache key to look up
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(CacheKey key) {
        return cache.containsKey(key.localKey());
    }
}
