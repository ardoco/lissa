# Caching System

## Overview

LiSSA relies on the caching subsystem provided by the [`io.github.ardoco:llm-access`](https://github.com/ardoco/llm-access) library to improve performance and ensure reproducibility of results. That library owns the cache abstraction and its implementations, so LiSSA no longer ships its own copies:

1. **Cache abstraction**: `Cache`, `CacheKey`, and `CacheParameter`, plus typed keys/parameters for chat (`ChatCacheKey` / `ChatCacheParameter`) and embedding (`EmbeddingCacheKey` / `EmbeddingCacheParameter`) operations.
2. **Cache implementations**: a hierarchical (two-level) cache with a conflict-resolution strategy, a file-based `LocalCache` (JSON, atomic writes, dirty tracking), a `RedisCache`, and a REST-based `RestRedisCache`.
3. **Cache management**: a `CacheManager` that configures the cache directory and provides cache instances keyed by origin and parameters.

See the [llm-access documentation](https://github.com/ardoco/llm-access) for the cache internals. The rest of this page describes how LiSSA configures and uses the cache (the configuration and environment variables are unchanged).

### Caching Usage

The caching system is used in several key components:

- **Embedding Creators**: cache vector embeddings to avoid recalculating them (keyed by `EmbeddingCacheParameter`: model name).
- **Classifiers**: cache LLM responses for classification tasks (keyed by `ChatCacheParameter`: model name, seed, temperature, content).
- **Preprocessors**: cache results of LLM-based preprocessing (keyed by `ChatCacheParameter`).

## Key Concepts

### Cache Keys

Cache keys uniquely identify cached items and consist of two parts:
- **JSON Key**: Serialized representation including all cache parameters (model, seed, temperature, content, mode)
- **Local Key**: Generated UUID-based key for in-memory identification and logging

### Cache Parameters

Cache parameters define the configuration that makes a cache unique:
- **ChatCacheParameter**: Model name, seed, and temperature for reproducible LLM results
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
   3. If Redis is unavailable, but configured to be used the system will fail.

4. **Best Practices**

   - Use the cache directory specified in the configuration
   - Clear the cache directory if you encounter issues
   - For production environments:
     - Use Redis for better performance
     - Configure Redis persistence for data durability
     - Monitor Redis memory usage
     - Set up Redis replication for high availability
   - Monitor cache size and implement cleanup strategies if needed

