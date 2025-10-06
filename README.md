# LiSSA: A Framework for Generic Traceability Link Recovery

<img src=".github/images/approach.svg" alt="Approach Overview" style="width: 100%;" /><br/>

Welcome to the LiSSA project!
This framework leverages Large Language Models (LLMs) enhanced through Retrieval-Augmented Generation (RAG) to establish traceability links across various software artifacts.

> [!TIP]
> If you have any questions, don't hesitate to [contact us](https://ardoco.de).

## Overview

In software development and maintenance, numerous artifacts such as requirements, code, and architecture documentation are produced.
Understanding the relationships between these artifacts is crucial for tasks like impact analysis, consistency checking, and maintenance.
LiSSA aims to provide a generic solution for Traceability Link Recovery (TLR) by utilizing LLMs in combination with RAG techniques.

The concept and evaluation of LiSSA are detailed in our paper:

> Fuchß, D., Hey, T., Keim, J., Liu, H., Ewald, N., Thirolf, T., & Koziolek, A. (2025). LiSSA: Toward Generic Traceability Link Recovery through Retrieval-Augmented Generation. In Proceedings of the IEEE/ACM 47th International Conference on Software Engineering, Ottawa, Canada.

You can access the paper [here](https://ardoco.de/c/icse25).

## Features

- **Generic Applicability**: LiSSA is designed to recover traceability links across various types of software artifacts, including:
  - [Requirements to code](https://ardoco.de/c/icse25)
  - [Documentation to code](https://ardoco.de/c/icse25)
  - [Architecture documentation to architecture models](https://ardoco.de/c/icse25)
  - [Requirements to requirements](https://ardoco.de/c/refsq25)
- **Retrieval-Augmented Generation**: By combining LLMs with RAG, LiSSA enhances the accuracy and relevance of the recovered traceability links.

## Documentation

The documentation is organized into several sections:

- [Architecture](docs/architecture.md): Detailed information about the project's architecture and components
- [Configuration](docs/configuration.md): Guide for configuring LiSSA
- [CLI Usage](docs/cli.md): Information about using the command line interface
- [Caching](docs/caching.md): Information about the caching system and Redis setup
- [Development](docs/development.md): Development setup and contribution guidelines

## Getting Started

To get started with LiSSA, follow these steps:

1. **Clone the Repository**:

   ```bash
   git clone https://github.com/ardoco/lissa-ratlr
   cd lissa-ratlr
   ```
2. **Install Dependencies**:
   Ensure you have Java JDK 21 or later installed. Then, build the project using Maven:

   ```bash
   mvn clean package
   ```
3. **Run LiSSA**:
   Execute the main application:

   ```bash
   java -jar target/ratlr-*-jar-with-dependencies.jar eval -c config.json
   ```

### Configuration

1. Create a configuration you want to use for evaluation / execution. E.g., you can find configurations [here](https://github.com/ArDoCo/ReplicationPackage-ICSE25_LiSSA-Toward-Generic-Traceability-Link-Recovery-through-RAG/tree/main/LiSSA-RATLR-V2/lissa/configs/req2code-significance). You can also provide a directory containing multiple configurations.
2. Configure your API keys for the language model platforms you plan to use as environment variables. See the [configuration documentation](docs/configuration.md#supported-platforms-and-environment-variables) for details on supported platforms (OpenAI, Open WebUI, Ollama, Blablador, DeepSeek) and their required environment variables.
3. LiSSA caches requests in order to be reproducible. The cache is located in the cache folder that can be specified in the configuration.
4. Run `java -jar target/ratlr-*-jar-with-dependencies.jar eval -c configs/....` to run the evaluation. You can provide a JSON or a directory containing JSON configurations.
5. The results will be printed to the console and saved to a file in the current directory. The name is also printed to the console.

### Results of Evaluation / Execution

The results will be stored as markdown files.
A result file can look like below.
It contains the configuration and the results of the evaluation.
Additionally, the LiSSA generate CSV files that contain the traceability links as pairs of identifiers.

<details>
<summary>Example Result</summary>

```json
## Configuration
{
  "cache_dir" : "./cache-r2c/dronology-dd--102959883",
  "gold_standard_configuration" : {
    "hasHeader" : false,
    "path" : "./datasets/req2code/dronology-dd/answer.csv"
  },
  "... other configuration parameters ..."
}

## Stats
* # TraceLinks (GS): 740
* # Source Artifacts: 211
* # Target Artifacts: 423
## Results
* True Positives: 283
* False Positives: 1286
* False Negatives: 457
* Precision: 0.18036966220522627
* Recall: 0.3824324324324324
* F1: 0.24512776093546992
```

</details>

## Evaluation

LiSSA has been empirically evaluated on four different TLR tasks:

- Requirements to code
- Documentation to code
- Architecture documentation to architecture models
- Requirements to requirements

The results indicate that the RAG-based approach can significantly outperform state-of-the-art methods in code-related tasks.
However, further research is needed to enhance its performance for broader applicability.

## Contributing

We welcome contributions from the community! If you're interested in contributing to LiSSA, please read our [Code of Conduct](CODE_OF_CONDUCT.md) and [Development Guide](docs/development.md) to get started.

## License

This project is licensed under the MIT License. See the [LICENSE.md](LICENSE.md) file for details.

## Acknowledgments

LiSSA is developed by researchers from the Modelling for Continuous Software Engineering (MCSE) group of KASTEL - Institute of Information Security and Dependability at the Karlsruhe Institute of Technology (KIT).

For more information about the project and related research, visit our [website](https://ardoco.de/).

---

*Note: This README provides a brief overview of the LiSSA project. For comprehensive details, please refer to our [documentation](docs/).*
