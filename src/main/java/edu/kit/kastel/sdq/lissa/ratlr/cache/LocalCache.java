/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implements a local file-based cache for storing key-value pairs.
 * This class provides a thread-safe implementation of a cache that persists its contents
 * to a JSON file. It includes automatic flushing of changes when a certain threshold
 * of modifications is reached.
 *
 * @param <K> The type of cache key used in this cache
 */
class LocalCache<K extends CacheKey> implements Cache<K> {

    private final ObjectMapper mapper;

    /**
     * Maximum number of modifications before automatic flush.
     */
    private static final int MAX_DIRTY = 50;

    private final CacheParameter<K> cacheParameter;

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
     * @param cacheParameter The cache parameter configuration
     */
    LocalCache(String cacheFile, CacheParameter<K> cacheParameter) {
        this.cacheParameter = Objects.requireNonNull(cacheParameter);
        this.cacheFile = new File(Objects.requireNonNull(cacheFile));
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

    @Override
    public synchronized <T> @Nullable T get(String key, Class<T> clazz) {
        K cacheKey = cacheParameter.createCacheKey(key);
        String jsonData = cache.get(cacheKey.localKey());
        return Cache.convert(jsonData, clazz, mapper);
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key The cache key to look up
     * @return The cached value, or null if not found
     * @deprecated This method exposes internal cache key handling and should not be used in general code.
     */
    @Override
    @Deprecated(forRemoval = false)
    public synchronized <T> @Nullable T getViaInternalKey(K key, Class<T> clazz) {
        String jsonData = cache.get(key.localKey());
        return Cache.convert(jsonData, clazz, mapper);
    }

    @Override
    public synchronized void put(String key, String value) {
        K cacheKey = cacheParameter.createCacheKey(key);
        putViaInternalKey(cacheKey, value);
    }

    /**
     * Stores a value in the cache.
     * If the value is different from the existing value (if any), the dirty counter is incremented.
     * If the dirty counter exceeds the maximum threshold, the cache is automatically flushed to disk.
     *
     * @param cacheKey The cache key to store the value under
     * @param value The value to store
     * @deprecated This method exposes internal cache key handling and should not be used in general code.
     */
    @Override
    @Deprecated(forRemoval = false)
    public synchronized <T> void putViaInternalKey(K cacheKey, T value) {
        String jsonValue;
        try {
            jsonValue = mapper.writeValueAsString(Objects.requireNonNull(value));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize object", e);
        }
        String old = cache.put(cacheKey.localKey(), jsonValue);
        if (old == null || !old.equals(jsonValue)) {
            dirty++;
        }

        if (dirty > MAX_DIRTY) {
            write();
        }
    }

    @Override
    public synchronized <T> void put(String key, T value) {
        K cacheKey = cacheParameter.createCacheKey(key);
        putViaInternalKey(cacheKey, value);
    }

    @Override
    public void flush() {
        write();
    }

    /**
     * Returns true if and only if this map contains a mapping for a key
     *
     * @param key The cache key to look up
     * @return true if this map contains a mapping for the specified key
     */
    @Override
    public synchronized boolean containsKey(String key) {
        K cacheKey = cacheParameter.createCacheKey(key);
        return cache.containsKey(cacheKey.localKey());
    }

    @Override
    public CacheParameter<K> getCacheParameter() {
        return this.cacheParameter;
    }
}
