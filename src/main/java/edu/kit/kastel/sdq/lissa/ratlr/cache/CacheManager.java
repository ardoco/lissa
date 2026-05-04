/* Licensed under MIT 2025-2026. */
package edu.kit.kastel.sdq.lissa.ratlr.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.kastel.sdq.lissa.ratlr.utils.Environment;

import redis.clients.jedis.exceptions.JedisConnectionException;

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
     * The default cache hierarchy: LOCAL only.
     */
    private static final String DEFAULT_CACHE_HIERARCHY = "LOCAL";

    /**
     * The default strategy for handling cache conflicts between local and Redis caches.
     */
    private static final CacheReplacementStrategy DEFAULT_REPLACEMENT_STRATEGY = CacheReplacementStrategy.ERROR;

    private static @Nullable CacheManager defaultInstanceManager;
    private final Path directoryOfCaches;
    private final CacheReplacementStrategy replacementStrategy;
    private final List<String> hierarchyConfig;
    private final Map<String, Cache<?>> caches = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

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
     * Reads the cache replacement strategy from environment variables.
     * This method:
     * <ol>
     *     <li>First checks the environment variable CACHE_REPLACEMENT_STRATEGY</li>
     *     <li>If not found, uses the default strategy ({@link #DEFAULT_REPLACEMENT_STRATEGY})</li>
     * </ol>
     *
     * @return The cache replacement strategy
     * @throws IllegalArgumentException If the environment variable value is set but invalid
     */
    private static CacheReplacementStrategy readCacheReplacementStrategy() {
        String strategyValue = Environment.getenv("CACHE_REPLACEMENT_STRATEGY");
        if (strategyValue == null) {
            return DEFAULT_REPLACEMENT_STRATEGY;
        }

        try {
            return CacheReplacementStrategy.valueOf(strategyValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid CACHE_REPLACEMENT_STRATEGY value: " + strategyValue + ". See "
                            + CacheReplacementStrategy.class + " for valid options.",
                    e);
        }
    }

    /**
     * Reads the cache hierarchy configuration from environment variables or uses the default if it's not set.
     *
     * @return The cache hierarchy configuration string
     */
    private static String readHierarchyString() {
        String hierarchyString = Environment.getenv("CACHE_HIERARCHY");
        if (hierarchyString == null) {
            return DEFAULT_CACHE_HIERARCHY;
        }
        return hierarchyString;
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
        this(cacheDir, readCacheReplacementStrategy(), parseCacheHierarchy(readHierarchyString()));
    }

    /**
     * Creates a new cache manager instance with the specified cache directory, replacement strategy, and cache
     * hierarchy configuration.
     * The directory will be created if it doesn't exist.
     *
     * @param cacheDir The path to the cache directory
     * @param replacementStrategy The strategy for handling conflicts between cache layers
     * @param hierarchyConfig The list of cache types in the hierarchy order
     * @throws IOException If the cache directory cannot be created
     * @throws IllegalArgumentException If the path exists but is not a directory
     */
    public CacheManager(Path cacheDir, CacheReplacementStrategy replacementStrategy, List<String> hierarchyConfig)
            throws IOException {
        if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
        if (!Files.isDirectory(cacheDir)) {
            throw new IllegalArgumentException("path is not a directory: " + cacheDir);
        }

        this.directoryOfCaches = cacheDir;
        this.replacementStrategy = replacementStrategy;
        this.hierarchyConfig = hierarchyConfig;
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
     * @param <K> The type of cache key used in this cache
     * @return A cache instance for the specified name
     */
    public <K extends CacheKey> Cache<K> getCache(Object origin, CacheParameter<K> parameters) {
        if (origin == null || parameters == null) {
            throw new IllegalArgumentException("Origin and parameters must not be null");
        }
        String name = origin.getClass().getSimpleName() + "_" + parameters.parameters();
        return getCache(name, parameters);
    }

    /**
     * Gets a cache instance for the specified name and parameters.
     *
     * @param name The name of the cache
     * @param parameters The parameters that define the cache configuration
     * @return A cache instance for the specified name
     */
    private <K extends CacheKey> Cache<K> getCache(String name, CacheParameter<K> parameters) {
        name = name.replace(":", "__");

        if (caches.containsKey(name)) {
            @SuppressWarnings("unchecked")
            Cache<K> cached = (Cache<K>) caches.get(name);
            if (!cached.getCacheParameter().equals(parameters)) {
                throw new IllegalArgumentException(
                        "Cache with name " + name + " already exists with different parameters");
            }
            return cached;
        }

        Cache<K> cache = buildCacheHierarchy(name, parameters);
        caches.put(name, cache);
        return cache;
    }

    /**
     * Builds a cache hierarchy based on the configured cache types.
     * The hierarchy is read from the CACHE_HIERARCHY environment variable.
     * Caches are layered in the order specified: the first cache is the primary layer,
     * the second is the secondary layer, etc.
     * If only one cache type is specified, it is returned directly without layering.
     *
     * @param <K> The type of cache key
     * @param cacheName The name of the cache
     * @param parameters The cache parameters
     * @return The configured cache instance
     */
    private <K extends CacheKey> Cache<K> buildCacheHierarchy(String cacheName, CacheParameter<K> parameters) {
        ObjectMapper mapper = new ObjectMapper();
        String cacheFilePath = directoryOfCaches.resolve(cacheName + ".json").toString();
        // Create cache instances for each type, skipping those that fail to initialize
        List<Cache<K>> createdCaches = new ArrayList<>();
        for (String cacheType : hierarchyConfig) {
            try {
                Cache<K> cache = Cache.createByType(cacheType, parameters, cacheFilePath, mapper);
                createdCaches.add(cache);
                logger.debug("Created cache type: {}", cacheType);
            } catch (JedisConnectionException e) {
                logger.warn(
                        "Failed to initialize cache type '{}': {}. Skipping this cache layer.",
                        cacheType,
                        e.getMessage());
            }
        }

        if (createdCaches.isEmpty()) {
            return new LocalCache<>(cacheFilePath, parameters);
        }

        Cache<K> layeredCache = createdCaches.getFirst();
        for (int i = 1; i < createdCaches.size(); i++) {
            layeredCache = new HierarchicalCache<>(parameters, layeredCache, createdCaches.get(i), replacementStrategy);
        }
        return layeredCache;
    }

    /**
     * Parses the cache hierarchy configuration string into a list of cache types.
     * The input should be a comma-separated list of cache types (case-insensitive).
     * Example: "LOCAL,REDIS" or "local,redis,my_cache"
     *
     * @param hierarchyConfig The hierarchy configuration string
     * @return A list of cache types in order
     * @throws IllegalArgumentException If the configuration is empty or invalid
     */
    // TODO: Support quotes (and spaces) like 'REDIS, LOCAL'
    private static List<String> parseCacheHierarchy(String hierarchyConfig) {
        String[] types = hierarchyConfig.split(",");
        List<String> cacheTypes = new ArrayList<>();

        for (String type : types) {
            String trimmed = type.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Cache hierarchy contains empty cache type");
            }
            cacheTypes.add(trimmed.toLowerCase());
        }
        return cacheTypes;
    }

    /**
     * Flushes all caches managed by this cache manager.
     * This ensures that all pending changes are written to disk.
     */
    public void flush() {
        for (Cache<?> cache : caches.values()) {
            cache.flush();
        }
    }

    /**
     * Resets the default cache manager instance.
     * This method is intended for testing purposes only to allow clean state between tests.
     * After calling this method, {@link #setCacheDir(String)}
     * must be called again before using the default instance.
     */
    static synchronized void resetDefaultInstance() {
        if (defaultInstanceManager != null) {
            defaultInstanceManager.flush();
        }
        defaultInstanceManager = null;
    }
}
