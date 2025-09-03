/* Licensed under MIT 2025. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import edu.kit.kastel.sdq.lissa.ratlr.knowledge.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Manages caching operations in the LiSSA framework.
 * This class provides a centralized way to create and access caches for different purposes,
 * such as storing embeddings or chat responses. It supports both local file-based caching
 * and Redis-based caching with automatic synchronization.
 */
public final class CacheManager {
    /**
     * The default directory name for storing cache files.
     */
    public static final String DEFAULT_CACHE_DIRECTORY = "cache";

    private static CacheManager defaultInstanceManager;
    private final Path directoryOfCaches;
    private final Map<String, RedisCache> caches = new HashMap<>();
    private final Map<KeyRegistration, CacheKey> keyRegistrations = new HashMap<>();

    /**
     * Sets the cache directory for the default cache manager instance.
     * This method must be called before using the default instance.
     *
     * @param directory The path to the cache directory, or null to use the default directory
     * @throws IOException If the cache directory cannot be created
     */
    public static synchronized void setCacheDir(String directory) throws IOException {
        defaultInstanceManager = new CacheManager(Path.of(directory == null ? DEFAULT_CACHE_DIRECTORY : directory));
    }

    /**
     * Creates a new cache manager instance using the specified cache directory.
     * The directory will be created if it doesn't exist.
     *
     * @param cacheDir The path to the cache directory
     * @throws IOException If the cache directory cannot be created
     * @throws IllegalArgumentException If the path exists but is not a directory
     */
    public CacheManager(Path cacheDir) throws IOException {
        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
        if (!Files.isDirectory(cacheDir)) {
            throw new IllegalArgumentException("path is not a directory: " + cacheDir);
        }
        this.directoryOfCaches = cacheDir;
    }

    /**
     * Gets the default cache manager instance.
     * The cache directory must be set using {@link #setCacheDir(String)} before calling this method.
     *
     * @return The default cache manager instance
     * @throws IllegalStateException If the cache directory has not been set
     */
    public static CacheManager getDefaultInstance() {
        if (defaultInstanceManager == null) throw new IllegalStateException("Cache directory not set");
        return defaultInstanceManager;
    }

    /**
     * Gets a cache instance for the specified name.
     * This method is designed for internal use by model implementations.
     * The cache name will be sanitized by replacing colons with double underscores.
     *
     * @param origin The class origin (caller, {@code this})
     * @param parameters a list of parameters that define what makes a cache unique. E.g., the model name, temperature, and seed.
     * @return A cache instance for the specified name
     */
    public Cache getCache(Object origin, String[] parameters) {
        if (origin == null || parameters == null) {
            throw new IllegalArgumentException("Origin and parameters must not be null");
        }
        for (String param : parameters) {
            if (param == null) {
                throw new IllegalArgumentException("Parameters must not contain null values");
            }
        }
        String name = origin.getClass().getSimpleName() + "_" + String.join("_", parameters);
        return getCache(name, true);
    }

    /**
     * Gets a cache instance for the specified name, optionally appending a file extension.
     *
     * @param name The name of the cache
     * @param appendEnding Whether to append the .json extension to the cache name
     * @return A cache instance for the specified name
     */
    private Cache getCache(String name, boolean appendEnding) {
        name = name.replace(":", "__");

        if (caches.containsKey(name)) {
            return caches.get(name);
        }

        LocalCache localCache = new LocalCache(directoryOfCaches + "/" + name + (appendEnding ? ".json" : ""));
        RedisCache cache = new RedisCache(localCache);
        caches.put(name, cache);
        return cache;
    }

    /**
     * Gets a cache instance for an existing cache file.
     *
     * @param path The path to the existing cache file
     * @param create Whether to create the cache file if it doesn't exist
     * @return A cache instance for the specified file
     * @throws IllegalArgumentException If the file doesn't exist (and create is false) or is a directory
     */
    public Cache getCache(Path path, boolean create) {
        path = directoryOfCaches.resolve(path.getFileName());
        if ((!create && Files.notExists(path)) || Files.isDirectory(path)) {
            throw new IllegalArgumentException("file does not exist or is a directory: " + path);
        }
        return getCache(path.getFileName().toString(), false);
    }

    /**
     * Flushes all caches managed by this cache manager.
     * This ensures that all pending changes are written to disk.
     */
    public void flush() {
        for (Cache cache : caches.values()) {
            cache.flush();
        }
        this.keyRegistrations.clear();
    }

    public CacheKey getKey(Element source, Element target) {
        return this.keyRegistrations.get(new KeyRegistration(source, target));
    }

    public void register(Element source, Element target, CacheKey cacheKey) {
        this.keyRegistrations.put(new KeyRegistration(source, target), cacheKey);
    }
    
    public record KeyRegistration(Element source, Element target) {
        
    }
}
