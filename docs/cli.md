# Command Line Interface

The packaged jar offers a CLI by [Picocli](https://picocli.info/) with the following features.

The build produces `target/lissa-<version>-jar-with-dependencies.jar` (see the [Development Guide](development.md)); the examples below use a glob to stay version-independent.

Each subcommand supports `-h, --help`. The top-level command does not, so use `java -jar target/lissa-*-jar-with-dependencies.jar eval --help` rather than `--help` on its own.

## Evaluation (Default)

Runs the pipeline and evaluates it against the ground truth.

### Examples

```bash
# Run with default configuration (./config.json)
java -jar target/lissa-*-jar-with-dependencies.jar eval

# Run with specific configuration file
java -jar target/lissa-*-jar-with-dependencies.jar eval -c ./config.json

# Run with multiple configurations
java -jar target/lissa-*-jar-with-dependencies.jar eval -c ./example-configs/simple-config.json ./example-configs/transitive

# Run with directory of configurations
java -jar target/lissa-*-jar-with-dependencies.jar eval -c ./example-configs
```

### Options

- `-c, --configs`: One or more config paths. If a path points to a directory, all files within it are processed recursively (up to a nesting depth of 50). If the option is omitted entirely, LiSSA falls back to `./config.json` in the current working directory; if that file does not exist either, the run logs a warning and does nothing.

> [!NOTE]
> A configuration that throws is logged as a warning and the run continues with the next one; the process still exits with code 0. Check the log rather than the exit code to confirm that a run succeeded.

## Evaluation (Transitive)

Runs the pipeline in transitive mode and evaluates it. This is useful for multi-step traceability link recovery.

### Examples

```bash
# Run transitive evaluation with multiple configurations
java -jar target/lissa-*-jar-with-dependencies.jar transitive -c ./example-configs/transitive/d2m.json ./example-configs/transitive/m2c.json -e ./example-configs/transitive/eval.json
```

### Options

- `-c, --configs`: Two or more config paths, invoked sequentially in the order given. Fewer than two paths aborts the run with an error.
- `-e, --evaluation-config`: **(Optional)** A single evaluation config path providing the gold standard for the transitive links. Note that this option is named `--evaluation-config` and takes exactly one path, unlike `--eval` of the `optimize` command. Without it, LiSSA only produces the transitive trace links and skips the evaluation.

## Prompt Optimization

Optimizes prompts used in trace link classification to improve performance.
This command runs the prompt optimization pipeline and optionally evaluates the optimized prompts against evaluation configurations.

The optimization process:
1. Runs baseline evaluation (if evaluation configs are provided)
2. Executes the prompt optimizer with the specified optimization configuration
3. Re-runs evaluation with the optimized prompt to measure improvement

As only the optimized prompt is transferred from the optimization results to the evaluation, other configuration parameters (e.g., model, dataset) do not have to match between optimization and evaluation configurations.

See the [Prompt Optimization Guide](prompt-optimization.md) for the pipeline itself.

### Examples

```bash
# Run optimization with a single config
java -jar target/lissa-*-jar-with-dependencies.jar optimize -c ./example-configs/optimizer-config.json

# Run optimization and evaluate the results
java -jar target/lissa-*-jar-with-dependencies.jar optimize -c ./example-configs/optimizer-config.json -e ./example-configs/simple-config.json

# Run optimization without any evaluation
java -jar target/lissa-*-jar-with-dependencies.jar optimize -c ./example-configs/optimizer-config.json -e

# Run optimization with directories of your own configs
java -jar target/lissa-*-jar-with-dependencies.jar optimize -c ./my-configs/optimization -e ./my-configs/evaluation
```

### Options

- `-c, --configs`: One or more optimization configuration file paths. If a path points to a directory, all files within it are processed recursively. If the option is omitted entirely, LiSSA falls back to `./config.json`, exactly as `eval` does.
- `-e, --eval`: Zero or more evaluation configuration file paths. Each evaluation configuration is used with each optimization config to measure performance before and after optimization.

> [!IMPORTANT]
> Both options fall back to `./config.json` when they are omitted. Leaving out `-e` therefore does **not** skip the evaluation if a `config.json` happens to sit in the working directory — it evaluates that file. Pass `-e` with no values to run the optimization without any evaluation.

