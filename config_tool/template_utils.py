"""Template handling helpers."""
from __future__ import annotations

from pathlib import Path
from typing import Dict

from constants import TEMPLATE_DIR
from helpers import ConfigDict, load_json


def available_templates() -> Dict[str, Path]:
    templates: Dict[str, Path] = {}
    for file in sorted(TEMPLATE_DIR.glob("*.json")):
        templates[file.name] = file
    return templates


def default_template_label() -> str:
    templates = available_templates()
    return next(iter(templates.keys()), "<no templates found>")


def initial_config() -> ConfigDict:
    templates = available_templates()
    if templates:
        first_template = next(iter(templates.values()))
        return load_json(first_template)
    return {
        "cache_dir": "./cache/example",
        "gold_standard_configuration": {
            "path": "./datasets/example/answer.csv",
            "hasHeader": "false",
        },
        "source_artifact_provider": {"name": "text", "args": {}},
        "target_artifact_provider": {"name": "text", "args": {}},
        "source_preprocessor": {"name": "artifact", "args": {}},
        "target_preprocessor": {"name": "artifact", "args": {}},
        "embedding_creator": {"name": "openai", "args": {"model": "text-embedding-3-large"}},
        "source_store": {"name": "custom", "args": {}},
        "target_store": {"name": "cosine_similarity", "args": {"max_results": "20"}},
        "classifier": {"name": "reasoning_openai", "args": {"model": "gpt-4o-mini-2024-07-18"}},
        "result_aggregator": {"name": "any_connection", "args": {}},
        "tracelinkid_postprocessor": {"name": "identity", "args": {}},
    }
