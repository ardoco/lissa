# Caching System

## Overview

LiSSA implements a sophisticated caching system to improve performance and ensure reproducibility of results. The caching system consists of the following components:

1. **Cache Interface** (`cache` package)
   - [`Cache`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/Cache.java): Core generic interface defining cache operations, parameterized by cache key type
   - [`CacheKey`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/CacheKey.java): Base interface for cache keys with JSON serialization support and local key generation
   - [`CacheParameter`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/CacheParameter.java): Interface defining cache configuration and key creation logic
   - **Specialized Cache Keys**:
     - [`ClassifierCacheKey`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/classifier/ClassifierCacheKey.java): Cache key for classifier operations (model name, seed, temperature, mode, content)
     - [`EmbeddingCacheKey`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/embedding/EmbeddingCacheKey.java): Cache key for embedding operations (model name, content)
   - **Cache Parameters**:
     - [`ClassifierCacheParameter`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/classifier/ClassifierCacheParameter.java): Configuration for classifier caches (model name, seed, temperature)
     - [`EmbeddingCacheParameter`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/embedding/EmbeddingCacheParameter.java): Configuration for embedding caches (model name)
2. **Cache Implementations**
   - [`Hierarchical Cache`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/HierarchicalCache.java): Two cache levels with a synchronization mechanism
     - Changes are applied to both levels
     - Reads use a Conflict Resolution Strategy to ensure consistent results
     - If a cache entry is missing in one level during a read, it is also written to the other level
   - [`LocalCache`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/LocalCache.java): File-based cache implementation that stores data in JSON format
     - Implements dirty tracking to optimize writes
     - Automatically saves changes on shutdown
     - Supports atomic writes using temporary files
   - [`RedisCache`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/RedisCache.java): Redis-based cache implementation
     - Uses Redis for high-performance caching
     - Supports both string and object serialization
   - [`RestRedisCache`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/RestRedisCache.java): REST-based Redis cache implementation
     - Uses REST API to interact with Redis server
     - Provides an alternative to direct Redis connections, useful for shared caches
     - Configuration through environment variables, see [Usage Instructions](#usage-instructions)
3. **Cache Management**
   - [`CacheManager`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/cache/CacheManager.java): Central manager for cache instances
     - Manages cache directory configuration
     - Provides singleton access to cache instances
     - Handles cache creation and retrieval based on origin and cache parameters
     - Ensures cache uniqueness by validating parameters
4. **Caching Usage**
   The caching system is used in several key components:
   - **Embedding Creators**: Caches vector embeddings to avoid recalculating them
     - Uses `EmbeddingCacheParameter` to identify unique embedding configurations
     - Cache keys are automatically generated based on content using the model name
   - **Classifiers**: Caches LLM responses for classification tasks
     - Uses `ClassifierCacheParameter` to identify unique classifier configurations
     - Cache keys include model name, seed, temperature, and content
   - **Preprocessors**: Caches preprocessing results for text summarization and other operations
     - Uses `ClassifierCacheParameter` for LLM-based preprocessing

## Key Concepts

### Cache Keys

Cache keys uniquely identify cached items and consist of two parts:
- **JSON Key**: Serialized representation including all cache parameters (model, seed, temperature, content, mode)
- **Local Key**: Generated UUID-based key for in-memory identification and logging

### Cache Parameters

Cache parameters define the configuration that makes a cache unique:
- **ClassifierCacheParameter**: Model name, seed, and temperature for reproducible LLM results
- **EmbeddingCacheParameter**: Model name only (embeddings are deterministic)

Parameters are used to:
1. Generate unique cache file names (via `parameters()` method)
2. Create cache keys from content (via `createCacheKey()` method)
3. Validate cache consistency when retrieving existing caches

### Cache Replacement Strategies

When using hierarchical caches with multiple layers (e.g., Redis and local cache), the system detects and resolves conflicts between layers:

- **NONE** (default): Does not replace conflicting values; leaves both cache layers as they are. Primary value is returned on read.
- **ERROR**: Throws an exception if a cache conflict is detected, ensuring data consistency by failing fast.
- **OVERWRITE**: Automatically overwrites the secondary cache value with the primary cache value when a conflict is detected, and logs a warning.

The replacement strategy for cache conflicts is configured via the `CACHE_REPLACEMENT_STRATEGY` environment variable.

### Cache API

The `Cache` interface provides two API levels:
1. **String-based API** (preferred): Pass content as string, cache handles key generation internally
- `get(String key, Class<T> clazz)`
- `put(String key, T value)`
- `containsKey(String key)`

2. **Internal Key API** (DO NOT USE): Direct cache key manipulation for special cases
   - `getViaInternalKey(K key, Class<T> clazz)`
   - `putViaInternalKey(K key, T value)`
   - Only use for backward compatibility or special handling scenarios

## Usage Instructions

1. **Configuration**

   ```json
   {
     "cache_dir": "./cache/path"  // Directory for cache storage
   }
   ```
2. **Environment Variables**

   The caching system supports the following environment variables:
   - **CACHE_HIERARCHY**: Comma-separated list of cache types in order (e.g., "LOCAL,REDIS")
   - Default: "LOCAL"
   - Supported values: "LOCAL", "REDIS", "REST_REDIS"
   - **CACHE_REPLACEMENT_STRATEGY**: Strategy for handling conflicts between cache layers
   - Default: "NONE"
   - Supported values: "NONE", "ERROR", "OVERWRITE"
   - **REDIS_URL**: Redis connection URL for RedisCache
   - Default: "redis://localhost:6379"
   - Example: "redis://redis-server:6379"
   - **REST_REDIS_URI**: URI for REST Redis server (if using REST_REDIS cache type)
   - **REST_REDIS_USERNAME**: Username for REST Redis authentication
   - **REST_REDIS_PASSWORD**: Password for REST Redis authentication

3. **Redis Setup**
   To use Redis for caching, you need to set up a Redis server. Here's a recommended Docker Compose configuration:

   ```yaml
   services:
     redis:
       image: redis/redis-stack:latest
       container_name: redis
       restart: unless-stopped
       ports:
         - "127.0.0.1:6379:6379"  # Redis server port
         - "127.0.0.1:5540:8001"  # RedisInsight web interface
       volumes:
         - ./redis_data:/data     # Persistent storage
   ```

   The Redis server will be available at `redis://localhost:6379`. You can also access the RedisInsight web interface at `http://localhost:5540` for monitoring and management.

   To use Redis with LiSSA:
   1. Start the Redis server using Docker Compose
   2. Set environment variables if needed:
   - `CACHE_HIERARCHY=REDIS,LOCAL` to use Redis with local fallback
   - `REDIS_URL=redis://your-redis-host:6379` if not using the default
   3. The system will automatically use Redis if available
   4. If Redis is unavailable, it will fall back to local file-based caching (useful for replication packages)

4. **Best Practices**

   - Use the cache directory specified in the configuration
   - Clear the cache directory if you encounter issues
   - For production environments:
     - Use Redis for better performance
     - Configure Redis persistence for data durability
     - Monitor Redis memory usage
     - Set up Redis replication for high availability
   - Monitor cache size and implement cleanup strategies if needed

