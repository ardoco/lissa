"""Loads definitions from the module catalog."""

from __future__ import annotations

from catalog import (
    artifact_provider_defs,
    classifier_mode_defs,
    classifier_platform_defs,
    embedding_defs,
    postprocessor_defs,
    preprocessor_defs,
    result_aggregator_defs,
    store_defs,
)


def load_catalog_sections():
    try:
        return {
            "artifact_providers": artifact_provider_defs(),
            "preprocessors": preprocessor_defs(),
            "embedding_creators": embedding_defs(),
            "source_store": store_defs("source_store"),
            "target_store": store_defs("target_store"),
            "result_aggregators": result_aggregator_defs(),
            "postprocessors": postprocessor_defs(),
            "classifier_modes": classifier_mode_defs(),
            "classifier_platforms": classifier_platform_defs(),
            "error": None,
        }
    except FileNotFoundError as exc:  # pragma: no cover - file required in repo
        return {"error": str(exc)}


CATALOG = load_catalog_sections()
