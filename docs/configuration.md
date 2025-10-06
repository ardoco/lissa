# Configuration Guide

## Overview

LiSSA uses JSON configuration files to define the behavior of the traceability link recovery process. This guide provides detailed information about available configuration options.

All pipeline components (artifact providers, preprocessors, embedding creators, classifiers, aggregators, and postprocessors) can access shared context via a ContextStore. This context mechanism is handled automatically by the framework and does not require explicit configuration in most cases.

## Finding Configuration Options

Configuration options in LiSSA are defined in the code through several mechanisms:

1. **Component Classes**: Each component (e.g., `ArtifactProvider`, `Preprocessor`, `Classifier`) has a corresponding class that defines its configuration options. For example:
   - [`TextArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/TextArtifactProvider.java) defines options for text-based artifact loading
   - [`CodeTreePreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeTreePreprocessor.java) defines options for code tree processing
   - [`OpenAiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenAiEmbeddingCreator.java) defines options for OpenAI embedding generation
   - [`OpenWebUiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenWebUiEmbeddingCreator.java) defines options for Open WebUI embedding generation
2. **Configuration Classes**: The [`Configuration`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/configuration/Configuration.java) class serves as the central configuration container, defining the structure of the configuration file.
3. **Example Configurations**: You can find example configurations in the `example-configs` directory, which demonstrate different configuration setups for various use cases.
4. **Configuration Template**: The `config-template.json` file provides a template with all available configuration options and their default values.

## Basic Configuration

```json
{
  "cache_dir": "./cache/path",  // Directory for caching results
  "gold_standard_configuration": {
    "path": "path/to/answer.csv",  // Path to ground truth file
    "hasHeader": false  // Whether the CSV has a header
  }
}
```

## Artifact Providers

```json
{
  "source_artifact_provider": {
    "name": "text",  // or "recursive_text"
    "args": {
      "artifact_type": "requirement",  // Type of artifact
      "path": "path/to/artifacts",  // Path to artifacts
      "extensions": "java"  // For recursive_text provider
    }
  }
}
```

## Preprocessors

```json
{
  "source_preprocessor": {
    "name": "artifact",  // or "code_tree", "code_chunking", etc.
    "args": {
      "language": "JAVA",  // For code processors
      "chunk_size": 60,    // For chunking
      "compare_classes": false,  // For code tree
      "includeUsages": true,  // For UML processor
      "includeOperations": true,  // For UML processor
      "includeInterfaceRealizations": true  // For UML processor
    }
  }
}
```

## Embedding and Classification

This section describes how to configure the embedding creation and classification steps. You must configure either a single `classifier` or a list of `classifiers` for multi-stage pipelines.

All classifier and embedding creator instances receive access to the shared ContextStore, enabling advanced scenarios such as sharing intermediate results or configuration between pipeline stages.

### Single Classifier

Use the `classifier` field to configure a single classifier.

```json
{
  "embedding_creator": {
    "name": "openai",  // or "ollama", "openwebui", "onnx", "mock"
    "args": {
      "model": "text-embedding-3-large"
    }
  },
  "classifier": {
    "name": "reasoning_openai",  // or "simple_openai", "reasoning_ollama", "simple_ollama", "reasoning_openwebui", "simple_openwebui", "reasoning_blablador", "simple_blablador", "reasoning_deepseek", "simple_deepseek", "mock"
    "args": {
      "model": "gpt-4o-mini-2024-07-18",
      ...  // Other classifier-specific arguments
    }
  }
}
```

### Multi-Stage Classifiers

Use the `classifiers` field to define a pipeline of classification stages. This field takes a list of lists of classifier configurations.

Each inner list represents a stage in the pipeline. Classifiers within the same stage are executed in parallel, and their results are aggregated using majority voting. The results of one stage are passed as input to the next stage.

```json
{
  "embedding_creator": {
    "name": "openai",  // or "ollama", "openwebui", "onnx", "mock"
    "args": {
      "model": "text-embedding-3-large"
    }
  },
  "classifiers": [
    // Stage 1
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
    // Stage 2
    [
      {
        "name": "reasoning_openai",
        "args": {
          "model": "gpt-4o-2024-05-13",
          // Additional arguments for the second stage
        }
      }
    ]
  ]
}
```

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

The retrieval of similar elements in the target store is now handled by a configurable retrieval strategy. The most common strategy is `cosine_similarity`, which finds the most similar elements based on cosine similarity of their embeddings. You can configure the retrieval strategy and its parameters in the `target_store` section.

```json
{
  "source_store": {
    "name": "custom",
    "args": {}
  },
  "target_store": {
    "name": "cosine_similarity",  // Retrieval strategy for finding similar elements
    "args": {
      "max_results": "20"  // Maximum number of similar elements to return, or "infinity"
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

- The `source_store` does not use a retrieval strategy and simply stores all source elements.
- The `target_store` must specify a retrieval strategy (currently, `cosine_similarity` is supported).
- The `max_results` argument controls how many similar elements are returned for each query. Use `"infinity"` to return all elements.

For more information about using the CLI to run configurations, see the [CLI documentation](cli.md).
