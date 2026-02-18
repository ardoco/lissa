# Prompt Optimization

## Overview

Prompt optimization in LiSSA-RATLR enables the automatic systematic refinement of prompts used for traceability link recovery.
By leveraging various optimization strategies and evaluation metrics, the effectiveness of prompts may be increased, leading to improved classification accuracy and overall performance.
This also enables us to quantify the importance of well designed prompts in the context of traceability link recovery.

## Core Components

### Prompt Metrics (`promptmetric` package)

A [`Metric`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptmetric/Metric.java) is a numeric measure used to evaluate the quality of prompts during the optimization process.
They are used to guide the optimization by providing feedback on how well a prompt performs in generating accurate traceability links.
Currently, they are divided into two types of metrics.
Global metrics evaluate the prompt's performance across the entire test dataset.
Pointwise metrics scores the performance of prompts on individual data points and reduces the results into a single numeric performance value.
If a pointwise metric is used, different scoring and reduction strategies can be configured and combined as desired.

Custom metrics can be added either through implementation of the [`Global Metrics`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptmetric/GlobalMetric.java) abstract class or through implementing new scoring and reduction strategies for pointwise metrics.

#### Available Metrics

- **[`Global Metrics`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptmetric/GlobalMetric.java)**:
  - **F_Beta-Score** (`fBeta` or `f1`)
- **[`Pointwise Metrics`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptmetric/PointwiseMetric.java)** (`pointwise`):
  - Scoring Strategies:
    - Binary Scorer (Correct Classification / Incorrect Classification)
  - Reduction Strategies:
    - Mean
- **[`Mock Metric`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptmetric/MockMetric.java)** (`mock`): Returns dummy values for testing purposes

### Optimizers (`promptoptimizer` package)

The [`Optimizer`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/PromptOptimizer.java) module handles prompt optimization requests.
Different optimization strategies are implemented to improve prompts using various means.
Optimization approaches will usually utilize an iterative process.
Prompts are refined over multiple iterations based on the feedback provided through the selected prompt metric.
They are highly configurable with the optimization configuration file.

Prompt optimizers utilize the usual stages of the evaluation pipeline as well.
They utilize LiSSA's caching mechanism to provide consistent and reproducible results across different runs.

Custom optimizers can be added by implementing the [`Prompt Optimizer`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/PromptOptimizer.java) interface.

#### Available Optimizers

- **[`Naive Iterative Optimizer`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/IterativeOptimizer.java)** (`iterative` or `simple`):
  The most basic optimizer that makes changes to the prompt in each iteration.
  It simply queries the large language model to improve the current prompt using an optimization prompt.
  The new prompt is naively carried over to the next iteration without any further checks.
  - `simple`: Defaults to one (1) iteration
  - `iterative`: Defaults to five (5) iterations
- **[`Feedback-Based Optimizer`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/IterativeFeedbackOptimizer.java)** (`feedback`):
  The iterative feedback optimizer improves prompts by leveraging feedback from the large language model.
  In each iteration, it queries the model with an additional feedback text on the current prompt.
  The optimizer carries the optimized prompt to the next iteration naively.
  Trace links that were incorrectly classified in previous iterations are highlighted in the feedback text to guide the model towards better performance.
- **[`Mock Optimizer`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/MockOptimizer.java)** (`mock`): Returns dummy optimized prompts for testing purposes

## Configuration

### Optimization Configuration Structure

Modules of the evaluation configuration file will also need to be configured in the optimization configuration file.
This excerpt shows the additional configuration options specific to prompt optimization.

```json

{
  [...]
  "metric" : {
    "name" : "mock",
    "args" : {}
  },
  "prompt_optimizer": {
    "name" : "simple_openai",
    "args" : {
      "prompt": "Question: Here are two parts of software development artifacts.\n\n            {source_type}: '''{source_content}'''\n\n            {target_type}: '''{target_content}'''\n            Are they related?\n\n            Answer with 'yes' or 'no'.",
      "model": "gpt-4o-mini-2024-07-18"
    }
  }
}

```

To see detailed configurable fields for any of the modules refer to a prompt optimization result file.
After executing a minimal configuration the resulting file will contain the full configuration with all default values filled in.

## Usage

Refer to the [CLI Documentation](cli.md#prompt-optimization) for instructions on how to run prompt optimization using the command line interface.

### Optimization Process

The optimization process generally follows these steps:

1. **Baseline Evaluation (Optional)**: If evaluation configurations are provided, the baseline performance of the original prompt is measured.
2. **Prompt Optimization**: The prompt optimizer is executed using the specified optimization configuration. The prompt is refined iteratively based on the selected metric.
3. **Post-Optimization Evaluation (Optional)**: If evaluation configurations are provided, the optimized prompt is evaluated to measure differences over the baseline.

## Output and Results

### Result Files

The prompt optimization results will be stored as `results-prompt-optimization-<config_filename>.md` just as regular evaluation results.
They include the full configuration used for optimization as well as the optimized prompt.
