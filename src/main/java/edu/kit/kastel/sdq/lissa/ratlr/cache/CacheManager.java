/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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

    /**
     * The default strategy for handling cache conflicts between local and Redis caches.
     * When true, Redis values take precedence over local cache values in case of conflicts.
     */
    private static final boolean DEFAULT_REPLACE_LOCAL_CACHE_ON_CONFLICT = true;

    private static @Nullable CacheManager defaultInstanceManager;
    private final Path directoryOfCaches;
    private final Map<String, RedisCache> caches = new HashMap<>();

    /**
     * Sets the cache directory for the default cache manager instance.
     * This method must be called before using the default instance.
     *
     * @param directory The path to the cache directory, or null to use the default directory
     * @throws IOException If the cache directory cannot be created
     */
    public static synchronized void setCacheDir(@Nullable String directory) throws IOException {
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
    public Cache getCache(Object origin, CacheParameter parameters) {
        if (origin == null || parameters == null) {
            throw new IllegalArgumentException("Origin and parameters must not be null");
        }
        String name = origin.getClass().getSimpleName() + "_" + parameters.parameters();
        return getCache(name);
    }

    /**
     * Gets a cache instance for the specified name, optionally appending a file extension.
     *
     * @param name The name of the cache
     * @return A cache instance for the specified name
     */
    private Cache getCache(String name) {
        name = name.replace(":", "__");

        if (caches.containsKey(name)) {
            return caches.get(name);
        }

        LocalCache localCache = new LocalCache(directoryOfCaches + "/" + name + ".json");
        RedisCache cache = new RedisCache(localCache, DEFAULT_REPLACE_LOCAL_CACHE_ON_CONFLICT);
        caches.put(name, cache);
        return cache;
    }

    /**
     * Flushes all caches managed by this cache manager.
     * This ensures that all pending changes are written to disk.
     */
    public void flush() {
        for (Cache cache : caches.values()) {
            cache.flush();
        }
    }
}
