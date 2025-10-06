# LiSSA Architecture

## Overview

LiSSA is a framework for generic traceability link recovery that uses Large Language Models (LLMs) enhanced through Retrieval-Augmented Generation (RAG). This guide provides detailed information about the project's architecture.

## Core Components

The project follows a modular architecture with the following main components:

1. **Artifact Providers** (`artifactprovider` package)
   - [`ArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/ArtifactProvider.java): Abstract base class for artifact providers
   - Implementations:
     - [`TextArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/TextArtifactProvider.java): Loads text-based artifacts from files, treating each file as a single artifact with its content and metadata.
     - [`RecursiveTextArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/RecursiveTextArtifactProvider.java): Recursively scans directories for files with specified extensions, creating artifacts for each matching file while preserving the directory structure.
2. **Preprocessors** (`preprocessor` package)
   - [`Preprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/Preprocessor.java): Abstract base class for preprocessing artifacts
   - Implementations:
     - [`SingleArtifactPreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/SingleArtifactPreprocessor.java): Treats each artifact as a single element without any splitting or transformation, useful for simple text documents.
     - [`CodeTreePreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeTreePreprocessor.java): Creates a hierarchical tree structure from code artifacts, organizing classes within their package context and maintaining parent-child relationships.
     - [`CodeChunkingPreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeChunkingPreprocessor.java): Splits code into fixed-size chunks while preserving context, useful for processing large code files.
     - [`CodeMethodPreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/CodeMethodPreprocessor.java): Uses TreeSitter to parse code and extract methods as individual elements, maintaining the class hierarchy.
     - [`ModelUMLPreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/ModelUMLPreprocessor.java): Processes UML models by extracting components, interfaces, and their relationships, with options to include usages, operations, and interface realizations.
     - [`SummarizePreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/SummarizePreprocessor.java): Uses LLMs to generate concise summaries of artifacts while preserving key information, with configurable templates for different artifact types.
     - [`SentencePreprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/SentencePreprocessor.java): Splits text documents into individual sentences while maintaining the original document as a parent element.
3. **Embedding Creators** (`embeddingcreator` package)
   - [`EmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/EmbeddingCreator.java): Base class for creating embeddings
   - Implementations:
     - [`OpenAiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenAiEmbeddingCreator.java): Uses OpenAI's embedding models to create vector representations of text, supporting various models like text-embedding-3-large.
     - [`OllamaEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OllamaEmbeddingCreator.java): Integrates with Ollama's local embedding models, providing an alternative to cloud-based solutions.
     - [`OpenWebUiEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OpenWebUiEmbeddingCreator.java): Integrates with Open WebUI servers for embedding generation, supporting local deployment with OpenAI-compatible APIs.
     - [`OnnxEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/OnnxEmbeddingCreator.java): Uses ONNX models for local embedding generation, offering high performance and offline capabilities.
     - [`MockEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/MockEmbeddingCreator.java): Provides zero vectors for testing purposes, useful for development and testing scenarios.
     - All extend [`CachedEmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/CachedEmbeddingCreator.java) for caching support, improving performance by storing and reusing embeddings.
4. **Element Stores** (`elementstore` package)
   - [`ElementStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/ElementStore.java): Manages storage and retrieval of processed elements with their embeddings, supporting similarity-based search and hierarchical relationships.
   - **Retrieval Strategies** (`elementstore/strategy` package):
     - [`RetrievalStrategy`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/strategy/RetrievalStrategy.java): Abstraction for finding similar elements in the target store. The retrieval strategy is configurable via the `target_store` section in the configuration file.
     - [`CosineSimilarity`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/strategy/CosineSimilarity.java): Default strategy that finds similar elements based on cosine similarity of embeddings. Supports the `max_results` parameter.
     - Retrieval strategies can be extended to implement custom similarity or retrieval logic.
5. **Classifiers** (`classifier` package)
   - [`Classifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/Classifier.java): Base class for classification
   - Implementations:
     - [`SimpleClassifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/SimpleClassifier.java): Uses a basic yes/no template with LLMs to determine relationships between elements, suitable for straightforward classification tasks.
     - [`ReasoningClassifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/ReasoningClassifier.java): Employs LLMs to provide detailed reasoning about relationships between elements, offering more nuanced classification decisions.
     - [`MockClassifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/MockClassifier.java): Always returns positive classification results, useful for testing and development purposes.
     - [`PipelineClassifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/PipelineClassifier.java): Implements a multi-stage classification process with majority voting, combining multiple classifiers for more robust results.
6. **Result Aggregators** (`resultaggregator` package)
   - [`ResultAggregator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/resultaggregator/ResultAggregator.java): Base class for result aggregation
   - Implementations:
     - [`AnyResultAggregator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/resultaggregator/AnyResultAggregator.java): Aggregates classification results based on configurable granularity levels, allowing for flexible relationship mapping between different levels of abstraction.
7. **Postprocessors** (`postprocessor` package)
   - [`TraceLinkIdPostprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/postprocessor/TraceLinkIdPostprocessor.java): Post-processes trace link IDs to ensure consistency and correctness in the final output, with specialized processors for different artifact types.
8. **Context** (`context` package)
   - [`Context`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/Context.java): Interface for context objects that can be registered and retrieved by ID.
   - [`ContextStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/ContextStore.java): Central registry for context objects, passed to all major pipeline components. Enables components to share state, configuration, or intermediate results, and supports advanced scenarios such as cross-component coordination and caching.

### Knowledge Model

The framework uses a hierarchical knowledge model:
- [`Knowledge`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/knowledge/Knowledge.java): Base class for all knowledge elements, providing common functionality for identification and content management.
- [`Artifact`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/knowledge/Artifact.java): Represents source artifacts (requirements, code, etc.) with their original content and metadata.
- [`Element`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/knowledge/Element.java): Represents processed artifacts or parts of artifacts with parent-child relationships, enabling hierarchical organization and granular analysis.

## Context Management

The pipeline uses a shared context mechanism to allow components to exchange additional information or state during execution:

- [`Context`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/Context.java): Interface for context objects that can be registered and retrieved by ID.
- [`ContextStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/ContextStore.java): Central registry for context objects, passed to all major pipeline components (artifact providers, preprocessors, embedding creators, classifiers, aggregators, and postprocessors). This enables components to share state or configuration as needed.

**Context handling is now managed in the superclasses of all pipeline components.** The `ContextStore` is a protected field in each superclass (e.g., `ArtifactProvider`, `Preprocessor`, `EmbeddingCreator`, `Classifier`, `ResultAggregator`, `TraceLinkIdPostprocessor`), and is automatically passed to all subclasses via their constructors. Subclasses should not duplicate context parameter documentation or handle context manually; instead, they inherit context access and documentation from their superclass.

The `ContextStore` is instantiated at the start of the pipeline and passed to all component factory methods. Components can register and retrieve context objects by unique ID, enabling advanced scenarios such as cross-component coordination, caching, or sharing of intermediate results.
