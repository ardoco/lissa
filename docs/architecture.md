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
   - [`EmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/EmbeddingCreator.java): LiSSA-side adapter that maps LiSSA `Element`s and configuration to the framework-neutral embedding creators of the [`io.github.ardoco:llm-access`](https://github.com/ardoco/llm-access) library.
   - The library provides the actual implementations (OpenAI, Ollama, ONNX, Open WebUI, and a mock), with token-length handling; all real creators cache their embeddings transparently, while the mock creator is not cached. The `openai` creator uses OpenAI embedding models such as `text-embedding-3-large`; `ollama`/`openwebui` integrate with local/OpenAI-compatible endpoints; `onnx` runs models locally for offline use.
4. **Element Stores** (`elementstore` package)
   - [`ElementStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/ElementStore.java): Abstract base holding elements with their embeddings, plus lookup by id and by parent id.
   - [`SourceElementStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/SourceElementStore.java): Holds the query side. It returns all elements (optionally only those flagged for comparison) and performs no similarity search. Configured via `source_store`, whose name must be `custom`.
   - [`TargetElementStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/TargetElementStore.java): Holds the candidate side and performs similarity search through a `RetrievalStrategy`. Configured via `target_store`.
   - [`ElementStoreOperations`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/elementstore/ElementStoreOperations.java): Utilities to reduce a source or target store to a subset, used by the prompt optimizers.
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
   - [`TraceLinkIdPostprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/postprocessor/TraceLinkIdPostprocessor.java): Post-processes trace link IDs to ensure consistency and correctness in the final output. The factory maps `req2code`, `sad2code`, `sad2sam`, `sam2sad` and `sam2code` onto this class parameterized by an `IdProcessor` enum value.
   - [`ReqReqPostprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/postprocessor/ReqReqPostprocessor.java): Used for the `req2req` name.
   - [`IdentityPostprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/postprocessor/IdentityPostprocessor.java): Used for the `identity` name, and when no postprocessor is configured at all.
   - Note: `ReqCodePostprocessor` exists in the package but is not reachable from the factory.
8. **Context** (`context` package)
   - [`Context`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/Context.java): Interface for context objects that can be registered and retrieved by ID.
   - [`ContextStore`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/context/ContextStore.java): Central registry for context objects, passed to all major pipeline components. It is an extension point for sharing state or intermediate results between components — see [Context Management](#context-management) for its current status.

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

The `ContextStore` is instantiated at the start of the pipeline and passed to all component factory methods. Components can register and retrieve context objects by unique ID, which is intended to enable cross-component coordination or sharing of intermediate results.

> [!NOTE]
> As of this release the mechanism is wired up but unused: no component registers or reads a context, and no implementation of the `Context` interface exists in the code base. Treat it as an extension point rather than as a feature in use.

## Pipeline

The component list above describes the building blocks; `Evaluation` is what orchestrates them. A run proceeds in this order:

1. Load artifacts from the source and target [`ArtifactProvider`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/artifactprovider/ArtifactProvider.java)s.
2. The [`Preprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/preprocessor/Preprocessor.java)s turn artifacts into elements.
3. The [`EmbeddingCreator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/embeddingcreator/EmbeddingCreator.java) computes embeddings for both element lists.
4. `SourceElementStore` and `TargetElementStore` are populated.
5. The [`Classifier`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/classifier/Classifier.java) pairs each source element with its similar targets and classifies each pair.
6. The [`ResultAggregator`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/resultaggregator/ResultAggregator.java) turns classification results into trace links at the configured granularity.
7. The [`TraceLinkIdPostprocessor`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/postprocessor/TraceLinkIdPostprocessor.java) rewrites the trace link IDs.
8. `Statistics` writes `results-*.md` and `traceLinks-*.csv` into the current working directory.
9. The cache manager is flushed.

Supporting classes that do not appear in the component list:

- `Evaluation` — the orchestrator described above; `Main` performs the same sequence inline and exists for testing.
- `Optimization` — reuses steps 1–4 and then runs a `PromptOptimizer` instead of steps 5–9. See [Prompt Optimization](prompt-optimization.md).
- `Statistics` — metric computation and result/trace-link output.
- The `cli` package — `MainCLI` and the `eval`, `transitive` and `optimize` subcommands. See [CLI Usage](cli.md).
- Caching — provided by the `llm-access` library (`CacheManager` and friends); see [Caching](caching.md).
- The `promptoptimizer` package — optimizers, metrics, selectors and sampling strategies.

