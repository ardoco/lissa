# Command Line Interface

The packaged jar offers a CLI by [Picocli](https://picocli.info/) with the following features.

## Evaluation (Default)

Runs the pipeline and evaluates it against the ground truth.

### Examples

```bash
# Run with default configuration
java -jar ./ratlr.jar eval

# Run with specific configuration file
java -jar ./ratlr.jar eval -c ./config.json

# Run with multiple configurations
java -jar ./ratlr.jar eval -c ./configs/simple.json ./configs/reasoning

# Run with directory of configurations
java -jar ./ratlr.jar eval -c ./configs
```

## Evaluation (Transitive)

Runs the pipeline in transitive mode and evaluates it. This is useful for multi-step traceability link recovery.

### Examples

```bash
# Run transitive evaluation with multiple configurations
java -jar ./ratlr.jar transitive -c ./configs/d2m.json ./configs/m2c.json -e ./configs/eval.json
```

## Prompt Optimization

Optimizes prompts used in trace link classification to improve performance.
This command runs the prompt optimization pipeline and optionally evaluates the optimized prompts against evaluation configurations.

The optimization process:
1. Runs baseline evaluation (if evaluation configs are provided)
2. Executes the prompt optimizer with the specified optimization configuration
3. Re-runs evaluation with the optimized prompt to measure improvement

As only the optimized prompt is transfered from the optimization results to the evaluation, other configuration parameters (e.g., model, dataset) do not have to match between optimization and evaluation configurations.

### Examples

```bash
# Run optimization with a single config
java -jar ./ratlr.jar optimize -c ./example-configs/optimizer-config.json

# Run optimization and evaluate the results
java -jar ./ratlr.jar optimize -c ./example-configs/optimizer-config.json -e ./example-configs/simple-config.json

# Run optimization with directories
java -jar ./ratlr.jar optimize -c ./configs/optimization -e ./configs/evaluation
```

### Options

- `-c, --configs`: **(Required)** One or more optimization configuration file paths. If a path points to a directory, all files within that directory will be processed.
- `-e, --eval`: **(Optional)** One or more evaluation configuration file paths. Each evaluation configuration will be used with each optimization config to measure performance before and after optimization.

