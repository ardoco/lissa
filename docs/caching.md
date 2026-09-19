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
     - Writes to disk when more than 50 entries have changed since the last write, and when the pipeline flushes the cache at the end of a run. There is **no shutdown hook** — if the JVM is killed mid-run, up to 50 new entries are lost
     - Writes via a temporary file that is then copied over the cache file (`REPLACE_EXISTING`); note this is a copy, not an atomic move
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
     - Model name, seed and temperature are encoded in the cache **file name**; the entry key inside a file-based cache is derived from the content alone. The Redis backends key entries by the full JSON key instead (see [Cache Keys](#cache-keys))
   - **Preprocessors**: Caches preprocessing results for text summarization and other operations
     - Uses `ClassifierCacheParameter` for LLM-based preprocessing

## Key Concepts

### Cache File Names

Each file-based cache is a single JSON file at `<cache_dir>/<SimpleClassName>_<parameters>.json`, where `<SimpleClassName>` is the **calling class** — `CacheManager.getCache(origin, params)` uses `origin.getClass().getSimpleName()` — and `<parameters>` comes from `CacheParameter.parameters()`:

- classifiers, summarizing preprocessors and prompt optimizers: `<model>_<seed>`, or `<model>_<seed>_<temperature>` when `temperature != 0.0` (the temperature is omitted at `0.0` for backward compatibility);
- embedding creators: `<model>`.

Any `:` in the resulting name is replaced by `__`. Examples:

```text
SimpleClassifier_gpt-4o-mini-2024-07-18_133742243.json
ReasoningClassifier_gpt-4o-mini_133742243_0.7.json
OpenAiEmbeddingCreator_text-embedding-3-large.json
```

Because the Java class name is part of the file name, renaming a class orphans its cache, and classifiers sharing an implementation class share a cache file. If `cache_dir` is absent from the configuration, `./cache` relative to the current working directory is used.

### Cache Keys

A cache key has two representations, and **which one is used depends on the backend**:

- **Local key** — `UUID.nameUUIDFromBytes(content.replace("\r\n", "\n").getBytes(UTF_8))`, i.e. a name-based UUID (version 3, MD5) over the request content only. This is the key under which entries are stored in the JSON files of `LocalCache`. Model, seed and temperature are **not** part of it; they are encoded in the file name (see above).
- **JSON key** — the full key object serialized with Jackson (model, seed, temperature, mode, content). This is the key used by `RedisCache` and `RestRedisCache`.

Consequence for replay: an entry in a shipped cache file matches only if the request text is byte-identical after CRLF normalization. Any whitespace, template or content change produces a different UUID and therefore a silent miss.

### What Goes Into a Classifier Cache Key

`SimpleClassifier` hashes the template after `{source_type}`, `{source_content}`, `{target_type}` and `{target_content}` have been substituted — nothing else.

`ReasoningClassifier` hashes a rendering of the whole chat message list, of the form

```text
[SystemMessage { text = "..." }, UserMessage { name = null contents = [TextContent { text = "..." }] }]
```

built with `dev.langchain4j.internal.Utils.quoted` — an **internal**, non-API class of langchain4j.

> [!WARNING]
> Reasoning-classifier cache keys change — and every entry then silently misses — if any of the following change: the langchain4j version (pinned in `pom.xml`; `Utils.quoted` carries no stability guarantee), the hard-coded system message, `use_system_message`, the prompt template (including edits to the built-in prompt enum, since `prompt` is an index into it), or the `artifact_type` values substituted into `{source_type}`/`{target_type}`.
> The key contains **no format-version field and no salt**, so an incompatible change cannot be detected — it only produces misses. When replaying an archived cache, do not upgrade langchain4j and do not reformat prompts.

### Cache Parameters

Cache parameters define the configuration that makes a cache unique:
- **ClassifierCacheParameter**: Model name, seed, and temperature for reproducible LLM results
- **EmbeddingCacheParameter**: Model name only (embeddings are deterministic)

Parameters are used to:
1. Generate unique cache file names (via `parameters()` method)
2. Create cache keys from content (via `createCacheKey()` method)
3. Validate cache consistency when retrieving existing caches

## Cache Misses Are Silent

A classifier cache miss is not an error. `Classifier.parallelClassify` runs up to 100 virtual threads (OpenAI and Blablador; 10 for Open WebUI, 1 for Ollama and DeepSeek) with **no per-task exception handling**, so a failed LLM call — for example an HTTP 401 from a placeholder API key — kills that worker thread. The remaining workers finish, the result list is silently short, and `results-*.md` / `traceLinks-*.csv` are still written, with degraded precision and recall. `EvaluateCommand` then catches every per-configuration exception and logs a warning, and the CLI never sets a non-zero exit code. **A run that fell back to the network and got 401s looks like a successful run.**

An embedding miss fails differently but is equally misleading: any embedding exception is rerouted through `tryToFixWithLength`, which reports `Token length was not too long. Don't know how to handle previous exception`. Read that as "the embedding call failed", usually a missing or invalid key.

To verify that a replay really came from the cache:

1. Compare the metrics against the published values.
2. Grep the log for `Classifying (` and `Calculating embedding for` — both are logged at INFO **only on a miss**. A fully cached run emits neither.
3. Confirm that the cache files' modification times and sizes are unchanged.
4. Check stderr for uncaught `Exception in thread ...` traces.

## Credentials Are Required Even for a Fully Cached Run

Provider clients are constructed eagerly, before any cache is consulted, and they validate their environment variables at construction time. A 100% cache-hit replay therefore still requires the provider's variables to be **present**, although their values are never used. For the OpenAI-based configurations, set:

```bash
OPENAI_ORGANIZATION_ID=DUMMY
OPENAI_API_KEY=DUMMY
```

This is exactly what `src/test/resources/.env-test` does for the offline end-to-end test. Using deliberately invalid values is recommended: it turns an unnoticed cache miss into a visible failure instead of a live API call.

> [!WARNING]
> Putting dummy values in `.env` is not enough if a real key is exported in your shell — the process environment wins over `.env`. Unset the real values first, or the replay will quietly call the live API on a cache miss:
>
> ```bash
> unset OPENAI_API_KEY OPENAI_ORGANIZATION_ID
> ```

## Where Results Are Written

Results are always written into the **current working directory**, never into `cache_dir`:

- `results-<config-file-name>_<uuid>.md`
- `traceLinks-<config-file-name>_<uuid>.csv`
- `results-prompt-optimization-<config-file-name>_<uuid>.md`

The `<uuid>` is a deterministic hash over the fully-resolved configuration, with all defaults filled in. Replaying the same configuration with the same build therefore produces exactly the same file names and **overwrites the existing files without warning**. Run replays from a scratch directory, not from a directory that holds published results. Conversely, if a code-level default changes between releases, the uuid changes and you silently get a *new* file instead of an updated one.

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
     "cache_dir": "./cache/path"
   }
   ```

   `cache_dir` is the directory for cache storage. It may be omitted, in which case `./cache` relative to the current working directory is used.

2. **Environment Variables**

   All variables below are read through `Environment`, which loads a `.env` file from the **current working directory**. The real process environment takes precedence: `.env` only supplies variables that are not already exported.

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
   - Default: "http://localhost:8080"
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
   3. If Redis is configured but unavailable, cache construction throws. Under the `eval` CLI this aborts that configuration only: the error is logged as a warning, the remaining configurations still run, and the process exits 0 — check the log, not the exit code.

4. **Best Practices**

   - Use the cache directory specified in the configuration
   - Clear the cache directory if you encounter issues
   - For production environments:
     - Use Redis for better performance
     - Configure Redis persistence for data durability
     - Monitor Redis memory usage
     - Set up Redis replication for high availability
   - Monitor cache size and implement cleanup strategies if needed

