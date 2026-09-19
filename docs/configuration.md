# Configuration Guide

## Overview

LiSSA uses JSON configuration files to define the behavior of the traceability link recovery process. This guide provides detailed information about available configuration options.

All pipeline components (artifact providers, preprocessors, embedding creators, classifiers, aggregators, and postprocessors) can access shared context via a ContextStore. This context mechanism is handled automatically by the framework and does not require explicit configuration.

## Strict Configuration

LiSSA rejects configurations it does not fully understand. Three rules govern this, and all three abort the run:

- **Unknown top-level keys** abort loading with a Jackson `UnrecognizedPropertyException`.
- **Unread `args` keys** abort the run. After a module is constructed, `ModuleConfiguration.finalizeForSerialization()` compares the keys you supplied against the keys the module actually read, and throws `IllegalStateException: Argument with key <key> not retrieved from configuration ...` for any leftover. Each module therefore accepts **only** the keys listed for it below — a typo or a key copied from a different module is fatal, not ignored.
- **A missing `args` object** causes a `NullPointerException`. Every module needs an `args`; use `"args": {}` for modules that take no arguments.

Configuration files are parsed as strict JSON: **comments are not supported**. The snippets in this guide are valid JSON and can be copied as-is.

Note that these rules cut both ways for reproducibility: an archived configuration keeps working only as long as every module still reads every key it contains. Removing an argument from a module is a breaking change even when the argument is ignorable.

## Finding Configuration Options

Configuration options in LiSSA are defined in the code through several mechanisms:

1. **Component Classes**: Each component (e.g., `ArtifactProvider`, `Preprocessor`, `Classifier`) has a corresponding class that defines its configuration options. For example:
   - [`TextArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/TextArtifactProvider.java) defines options for text-based artifact loading
   - [`CodeTreePreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeTreePreprocessor.java) defines options for code tree processing
   - [`OpenAiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenAiEmbeddingCreator.java) defines options for OpenAI embedding generation
   - [`OpenWebUiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenWebUiEmbeddingCreator.java) defines options for Open WebUI embedding generation
2. **Configuration Classes**: The [`EvaluationConfiguration`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/configuration/EvaluationConfiguration.java) class is the central configuration container, defining the structure of an evaluation configuration file. [`OptimizerConfiguration`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/configuration/OptimizerConfiguration.java) does the same for prompt-optimization configurations, and [`Configuration`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/configuration/Configuration.java) is the marker interface both implement.
3. **Example Configurations**: You can find example configurations in the `example-configs` directory, which demonstrate different configuration setups for various use cases.
4. **Configuration Template**: The `config-template.json` file provides a starting point for a requirement-to-requirement pipeline with placeholder paths. It is not an exhaustive list of options and cannot be run before its `<<PLACEHOLDER>>` values are replaced.

> [!NOTE]
> Evaluation configurations and optimization configurations are different schemas. A file containing `metric`, `selector` or `prompt_optimizer` belongs to `lissa optimize` and fails under `lissa eval`; a gold-standard file such as `example-configs/transitive/eval.json` is neither. Pointing `eval -c` at a directory that mixes them logs a warning for each file that does not fit.

## Top-Level Structure

|                          Key                           |          Type          |          Required          |                    Default                    |
|--------------------------------------------------------|------------------------|----------------------------|-----------------------------------------------|
| `cache_dir`                                            | string                 | optional                   | `"cache"` (relative to the working directory) |
| `gold_standard_configuration`                          | object                 | optional                   | none — statistics are skipped when absent     |
| `source_artifact_provider`, `target_artifact_provider` | module                 | **required**               | –                                             |
| `source_preprocessor`, `target_preprocessor`           | module                 | **required**               | –                                             |
| `embedding_creator`                                    | module                 | **required**               | –                                             |
| `source_store`, `target_store`                         | module                 | **required**               | –                                             |
| `classifier` *or* `classifiers`                        | module / list of lists | **exactly one of the two** | –                                             |
| `result_aggregator`                                    | module                 | **required**               | –                                             |
| `tracelinkid_postprocessor`                            | module                 | optional                   | `identity`                                    |

## Basic Configuration

```json
{
  "cache_dir": "./cache/path",
  "gold_standard_configuration": {
    "path": "path/to/answer.csv",
    "hasHeader": false,
    "swap_columns": false
  }
}
```

- `cache_dir` — directory for the cache files. If omitted, `./cache` relative to the current working directory is used. See the [caching documentation](caching.md).
- `gold_standard_configuration.path` — CSV of `source,target` pairs used as ground truth.
- `hasHeader` (default `false`) — skips the first row of the CSV.
- `swap_columns` (default `false`) — reads the CSV as `target,source` instead.

`hasHeader` and `swap_columns` accept a JSON boolean or a quoted string (`"true"`), so configurations written either way keep parsing. The key is spelled `swap_columns` in snake_case; the camelCase spelling `swapColumns` is rejected. The whole `gold_standard_configuration` object may be omitted, in which case the pipeline still produces trace links but no statistics.

## Artifact Providers

The `text` provider reads the files of a single directory:

```json
{
  "source_artifact_provider": {
    "name": "text",
    "args": {
      "artifact_type": "requirement",
      "path": "path/to/artifacts"
    }
  }
}
```

The `recursive_text` provider walks a directory tree and filters by file extension:

```json
{
  "target_artifact_provider": {
    "name": "recursive_text",
    "args": {
      "artifact_type": "source code",
      "path": "path/to/code",
      "extensions": "java,kt"
    }
  }
}
```

|         Provider         |    Argument     |   Required   |                                              Notes                                               |
|--------------------------|-----------------|--------------|--------------------------------------------------------------------------------------------------|
| `text`, `recursive_text` | `artifact_type` | **required** | see the artifact types below                                                                     |
| `text`, `recursive_text` | `path`          | **required** | a non-existent path aborts the run                                                               |
| `recursive_text` only    | `extensions`    | **required** | comma-separated; matched with a case-insensitive `endsWith`, so `"java"` and `".java"` both work |

`extensions` is only read by `recursive_text` — passing it to `text` aborts the run.

Valid `artifact_type` values are `requirement`, `source_code` (also accepted as `"source code"`), `software_architecture_documentation`, and `software_architecture_model`. Matching is case-insensitive and spaces are interchangeable with underscores.

## Preprocessors

Each preprocessor reads a different set of arguments. Because unread arguments abort the run, the arguments of one preprocessor cannot be passed to another.

|          Name          |                              Arguments                               |                    Default                    |
|------------------------|----------------------------------------------------------------------|-----------------------------------------------|
| `artifact`             | none — use `"args": {}`                                              | –                                             |
| `sentence`             | none — use `"args": {}`                                              | –                                             |
| `code_chunking`        | `language` (**required**), `chunk_size`                              | `chunk_size`: `60`                            |
| `code_method`          | `language`                                                           | `"JAVA"`                                      |
| `code_tree`            | `language`, `compare_classes`                                        | `"JAVA"`, `false`                             |
| `model_uml`            | `includeUsages`, `includeOperations`, `includeInterfaceRealizations` | all `true`                                    |
| `summarize_<platform>` | `template`, plus the common model arguments                          | `"Summarize the following {type}: {content}"` |

```json
{
  "source_preprocessor": {
    "name": "artifact",
    "args": {}
  },
  "target_preprocessor": {
    "name": "code_chunking",
    "args": {
      "language": "JAVA",
      "chunk_size": 60
    }
  }
}
```

The `language` argument differs between the code preprocessors:

- `code_chunking` — **required**, uppercase, `JAVA` or `PYTHON`. Several languages may be given comma-separated for a mixed corpus. Lowercase values such as `"java"` are rejected.
- `code_tree` and `code_method` — optional, default `JAVA`, and `JAVA` is the **only** supported value.

The `model_uml` preprocessor reads a UML model and accepts three boolean flags:

```json
{
  "target_preprocessor": {
    "name": "model_uml",
    "args": {
      "includeUsages": true,
      "includeOperations": true,
      "includeInterfaceRealizations": true
    }
  }
}
```

`summarize` must carry a platform suffix, exactly like a classifier (e.g. `summarize_openai`); the bare name `summarize` is rejected. The bare names `code` and `model` are likewise invalid — use the full names from the table.

## Embedding and Classification

This section describes how to configure the embedding creation and classification steps. You must configure either a single `classifier` or a list of `classifiers` for multi-stage pipelines.

### Embedding Creators

|    Name     |                            Arguments                             |      Default model       |
|-------------|------------------------------------------------------------------|--------------------------|
| `openai`    | `model`                                                          | `text-embedding-ada-002` |
| `ollama`    | `model`                                                          | `nomic-embed-text:v1.5`  |
| `openwebui` | `model`                                                          | `nomic-embed-text:v1.5`  |
| `onnx`      | `model`, `path_to_model`, `path_to_tokenizer` — all **required** | –                        |
| `mock`      | none — use `"args": {}`                                          | –                        |

### Single Classifier

Use the `classifier` field to configure a single classifier.

```json
{
  "embedding_creator": {
    "name": "openai",
    "args": {
      "model": "text-embedding-3-large"
    }
  },
  "classifier": {
    "name": "reasoning_openai",
    "args": {
      "model": "gpt-4o-mini-2024-07-18",
      "seed": 133742243,
      "temperature": 0.0,
      "prompt": "0",
      "use_original_artifacts": false,
      "use_system_message": true
    }
  }
}
```

Classifier names are `mock`, `simple_<platform>` or `reasoning_<platform>`, where `<platform>` is one of `openai`, `ollama`, `openwebui`, `blablador`, `deepseek`. A `simple` or `reasoning` name without a platform suffix is rejected.

**Arguments common to `simple_*` and `reasoning_*`:**

|   Argument    |  Type  |                                                                   Default                                                                   |
|---------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `model`       | string | per platform: `gpt-4o-mini` (openai), `llama3:8b` (ollama, openwebui), `2 - Llama 3.3 70B instruct` (blablador), `deepseek-chat` (deepseek) |
| `seed`        | int    | `133742243`                                                                                                                                 |
| `temperature` | double | `0.0`                                                                                                                                       |

**`reasoning_*` only:**

- `prompt` (default `0`) — either a **numeric index into the built-in prompt templates** or **literal prompt text**. A value that parses as a number selects a built-in template; anything else is used verbatim as the prompt. The built-in templates are `0` (ask for reasoning, then yes/no), `1` (ask whether a link is *conceivable*), and `2` (answer yes only if absolutely certain). An index outside `0`–`2` aborts the run. Placeholders available in a custom prompt are `{source_type}`, `{source_content}`, `{target_type}`, `{target_content}`.
- `use_original_artifacts` (default `false`)
- `use_system_message` (default `true`)

**`simple_*` only:**

- `template` (default: a built-in yes/no template) — note that the simple classifier uses `template`, **not** `prompt`. Passing `prompt` to a `simple_*` classifier aborts the run.

`mock` accepts no arguments at all.

> [!IMPORTANT]
> Changing `prompt`, `use_system_message`, `artifact_type`, or the built-in prompt templates changes the derived cache keys, so an existing cache silently stops matching. See [Caching](caching.md) before editing these in a configuration whose cache you intend to reuse.

### Multi-Stage Classifiers

Use the `classifiers` field to define a pipeline of classification stages. This field takes a list of lists of classifier configurations.

Each inner list represents a stage in the pipeline. The classifiers of one stage are applied one after another, their results are aggregated by majority vote (a pair needs at least `ceil(n/2)` positive votes), and the surviving pairs are passed to the next stage.

```json
{
  "embedding_creator": {
    "name": "openai",
    "args": {
      "model": "text-embedding-3-large"
    }
  },
  "classifiers": [
    [
      {
        "name": "simple_openai",
        "args": {
          "model": "gpt-4o-mini-2024-07-18"
        }
      },
      {
        "name": "reasoning_openai",
        "args": {
          "model": "gpt-4o-mini-2024-07-18"
        }
      }
    ],
    [
      {
        "name": "reasoning_openai",
        "args": {
          "model": "gpt-4o-2024-05-13"
        }
      }
    ]
  ]
}
```

Setting both `classifier` and `classifiers`, or neither, aborts the run.

## Supported Platforms and Environment Variables

LiSSA supports multiple platforms for embedding creation and language model classification. Each platform requires specific environment variables to be configured:

### Embedding Creators

- **openai**: OpenAI's embedding models
  - `OPENAI_ORGANIZATION_ID`: Your OpenAI organization ID
  - `OPENAI_API_KEY`: Your OpenAI API key
- **ollama**: Local Ollama embedding models
  - `OLLAMA_EMBEDDING_HOST`: The host URL for the Ollama server (required)
  - `OLLAMA_EMBEDDING_USER`: Username for authentication (optional)
  - `OLLAMA_EMBEDDING_PASSWORD`: Password for authentication (optional)
- **openwebui**: Open WebUI embedding models
  - `OPENWEBUI_URL`: The URL of the Open WebUI server
  - `OPENWEBUI_API_KEY`: Your Open WebUI API key
- **onnx**: Local ONNX models (no environment variables required)
- **mock**: Mock embedding creator for testing (no environment variables required)

### Chat Language Models

Chat language models are configured by prefixing the classifier name with the platform. For example, `simple_openai`, `reasoning_ollama`, `simple_openwebui`, etc.

- **OpenAI** (`*_openai`): Uses OpenAI's chat models
  - `OPENAI_ORGANIZATION_ID`: Your OpenAI organization ID
  - `OPENAI_API_KEY`: Your OpenAI API key
- **Ollama** (`*_ollama`): Uses local Ollama chat models
  - `OLLAMA_HOST`: The host URL for the Ollama server (required)
  - `OLLAMA_USER`: Username for authentication (optional)
  - `OLLAMA_PASSWORD`: Password for authentication (optional)
- **Open WebUI** (`*_openwebui`): Uses Open WebUI chat models
  - `OPENWEBUI_URL`: The URL of the Open WebUI server
  - `OPENWEBUI_API_KEY`: Your Open WebUI API key
- **Blablador** (`*_blablador`): Uses Blablador's chat models
  - `BLABLADOR_API_KEY`: Your Blablador API key
- **DeepSeek** (`*_deepseek`): Uses DeepSeek's chat models
  - `DEEPSEEK_API_KEY`: Your DeepSeek API key

> [!IMPORTANT]
> These variables are validated when the client is constructed, before any cache is consulted. A run that is served entirely from the cache therefore still needs them to be set — dummy values are sufficient and open no connection. Variables are read through a `.env` file in the **current working directory** first, and only then from the process environment, so a stale `.env` silently overrides an exported variable.

### Example Configuration with Open WebUI

```json
{
  "embedding_creator": {
    "name": "openwebui",
    "args": {
      "model": "nomic-embed-text:v1.5"
    }
  },
  "classifier": {
    "name": "simple_openwebui",
    "args": {
      "model": "llama3:8b",
      "seed": 133742243,
      "temperature": 0.0
    }
  }
}
```

## Stores and Aggregation

The retrieval of similar elements in the target store is handled by a configurable retrieval strategy. The most common strategy is `cosine_similarity`, which finds the most similar elements based on cosine similarity of their embeddings.

```json
{
  "source_store": {
    "name": "custom",
    "args": {}
  },
  "target_store": {
    "name": "cosine_similarity",
    "args": {
      "max_results": "20"
    }
  },
  "result_aggregator": {
    "name": "any_connection",
    "args": {
      "source_granularity": 0,
      "target_granularity": 0
    }
  }
}
```

- The `source_store` stores all source elements and uses no retrieval strategy. Its name must be `custom` — any other name logs an error — and it reads no arguments, so `args` must be `{}`. Passing `max_results` here aborts the run.
- The `target_store` accepts `cosine_similarity` (the current name) or `custom`. **`custom` is a backwards-compatibility alias** that resolves to `cosine_similarity` and logs `For backwards compatibility: Using cosine similarity as default retrieval strategy.` on every run; prefer `cosine_similarity` in new configurations. Both names are case-sensitive.
- `max_results` (default `10`) controls how many similar elements are returned for each query. It must be an integer of at least 1, or the string `"infinity"` (case-insensitive) to return all elements. A value of `0` or a negative value aborts the run.
- `any_connection` is the only result aggregator. `source_granularity` and `target_granularity` both default to `0`.

## Trace Link ID Postprocessor

```json
{
  "tracelinkid_postprocessor": {
    "name": "identity",
    "args": {}
  }
}
```

Accepted names are `identity`, `req2code`, `req2req`, `sad2code`, `sad2sam`, `sam2sad`, and `sam2code`. None of them reads any arguments, so `args` must be `{}`. The whole field may be omitted, in which case `identity` is used — note that this silently changes the format of the produced trace link IDs compared to a configuration that sets an explicit postprocessor.

For more information about using the CLI to run configurations, see the [CLI documentation](cli.md).
