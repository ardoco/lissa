# Prompt Optimization

## Overview

Prompt optimization in LiSSA-RATLR enables the automatic systematic refinement of prompts used for traceability link recovery.
By leveraging various optimization strategies and evaluation metrics, the effectiveness of prompts may be increased, leading to improved classification accuracy and overall performance.
This also enables us to quantify the importance of well designed prompts in the context of traceability link recovery.

## Core Components

### Overview of Prompt Optimization Subcomponents

The table below provides a brief overview of the subcomponents used in the prompt optimization module.

|       Component        |        **SampleStrategy**        |                  **Selector**                  |           **Metric**           |
|------------------------|----------------------------------|------------------------------------------------|--------------------------------|
| **Location**           | `promptoptimizer.samplestrategy` | `promptoptimizer.promptselector`               | `promptoptimizer.promptmetric` |
| **Purpose**            | Select items from a collection   | Orchestrate prompt evaluation with budget      | Calculate performance scores   |
| **Answers**            | "Which items to use?"            | "Which prompts to test when?"                  | "How good is this prompt?"     |
| **Method**             | `sample(items, sampleSize)`      | `sampleAndEvaluate(prompts, examples, metric)` | `getMetric(prompts, examples)` |
| **Algorithm Examples** | First/Ordered/Shuffled           | Simple/UCB Bandit                              | Pointwise/FBeta                |

### Sample Strategies (`samplestrategy` package)

A [`SampleStrategy`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/SampleStrategy.java) determines how to select a subset of items from a collection.
These strategies are used throughout the optimization process to sample classification tasks, prompts, or other collections when the full set would be too large or expensive to process.
The key method `sample(items, sampleSize)` returns a list of selected items based on the strategy's selection logic.

Custom sample strategies can be added by implementing the [`SampleStrategy`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/SampleStrategy.java) interface and registering them in the [`SamplerFactory`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/SamplerFactory.java).

#### Available Sample Strategies

- **[`First Sampler`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/FirstSampler.java)** (`first`):
  Selects the first n items from the collection without any modification.
  Maintains the original order of items.
- **[`Ordered First Sampler`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/OrderedFirstSampler.java)** (`ordered`):
  Sorts items before selecting the first n items.
  Ensures deterministic sampling based on the natural ordering of items.
- **[`Shuffled First Sampler`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/samplestrategy/ShuffledFirstSampler.java)** (`shuffled`):
  Randomly shuffles items before selecting the first n items.
  Provides random sampling with reproducibility through seeded random number generation.

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

### Selectors (`promptselector` package)

A [`Selector`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptselector/Selector.java) orchestrates the evaluation of multiple prompts within a given evaluation budget.
They determine which prompts to test and when, managing the trade-off between exploration (testing new prompts) and exploitation (focusing on promising prompts).
Selectors use the `sampleAndEvaluate` method to coordinate prompt evaluation, calling the metric to score prompts against classification examples while respecting budget constraints.

The evaluation budget is calculated as `samplesPerEval × evalRounds × evalPromptsPerRound`, controlling how many total evaluations can be performed.
This budget management is crucial for expensive LLM-based evaluations.

Custom selectors can be added by extending the [`Selector`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptselector/Selector.java) abstract class.

#### Available Selectors

- **[`Simple Selector`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptselector/SimpleSelector.java)** (`simple`):
  Evaluates all provided prompts exhaustively against a subset of examples.
  The sample size is determined by dividing the evaluation budget by the number of prompts.
  Examples are shuffled randomly before selection to ensure diverse evaluation.
- **[`Upper Confidence Bound Bandit Selector`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptselector/UpperConfidenceBoundBanditSelector.java)** (`ucb`):
  Implements a multi-armed bandit approach using the UCB (Upper Confidence Bound) algorithm.
  Balances exploration and exploitation by selecting prompts based on both their current performance and uncertainty.
  More efficient than brute force when evaluating many prompts, as it focuses on promising candidates.
- **[`Mock Selector`](../src/main/java/edu/kit/kastel/sdq/lissa/ratlr/promptoptimizer/promptselector/MockSelector.java)** (`mock`): Returns dummy scores for testing purposes

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
