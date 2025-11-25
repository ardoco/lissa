"""Helpers to access module_catalog.json metadata."""
from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional
import json

CATALOG_PATH = Path(__file__).with_name("module_catalog.json")

CatalogDict = Dict[str, Any]


@lru_cache(maxsize=1)
def load_catalog() -> CatalogDict:
    """Return the parsed catalog JSON. Raises FileNotFoundError if missing."""
    with CATALOG_PATH.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _index_by_name(entries: Iterable[Dict[str, Any]]) -> Dict[str, Dict[str, Any]]:
    return {entry.get("name"): entry for entry in entries}


def artifact_provider_defs() -> Dict[str, Dict[str, Any]]:
    return _index_by_name(load_catalog().get("artifact_providers", []))


def preprocessor_defs() -> Dict[str, Dict[str, Any]]:
    return _index_by_name(load_catalog().get("preprocessors", []))


def embedding_defs() -> Dict[str, Dict[str, Any]]:
    return _index_by_name(load_catalog().get("embedding_creators", []))


def store_defs(kind: str) -> Dict[str, Dict[str, Any]]:
    stores = load_catalog().get("stores", {})
    return _index_by_name(stores.get(kind, []))


def classifier_mode_defs() -> Dict[str, Dict[str, Any]]:
    catalog = load_catalog().get("classifiers", {})
    return _index_by_name(catalog.get("modes", []))


def classifier_platform_defs() -> Dict[str, Dict[str, Any]]:
    return load_catalog().get("classifiers", {}).get("platforms", {})


def result_aggregator_defs() -> Dict[str, Dict[str, Any]]:
    return _index_by_name(load_catalog().get("result_aggregators", []))


def postprocessor_defs() -> Dict[str, Dict[str, Any]]:
    return _index_by_name(load_catalog().get("tracelink_postprocessors", []))


def _resolve_pointer(pointer: Any) -> Any:
    if not isinstance(pointer, str) or not pointer.startswith("@"):
        return pointer
    target = pointer[1:]
    node: Any = load_catalog()
    for part in target.split("."):
        if isinstance(node, dict):
            node = node.get(part)
        else:
            node = None
        if node is None:
            break
    return node


def describe_args(args: Dict[str, Any]) -> List[str]:
    """Return human readable bullet strings for an args block."""
    items: List[str] = []
    for key, meta in args.items():
        meta = meta or {}
        arg_type = meta.get("type", "string")
        default = meta.get("default")
        required = meta.get("required", False)
        description = meta.get("description", "")
        values = meta.get("values")
        resolved_values = _resolve_pointer(values)

        default_part = f", default={default}" if default is not None else ""
        required_part = " (required)" if required else ""
        values_part = ""
        if resolved_values is not None:
            if isinstance(resolved_values, (list, tuple, set)):
                preview = ", ".join(str(v) for v in resolved_values)
            else:
                preview = str(resolved_values)
            source_hint = f" via {values}" if isinstance(values, str) and values.startswith("@") else ""
            values_part = f", choices=[{preview}]{source_hint}"

        entry = (
            f"- `{key}` [{arg_type}{required_part}{default_part}{values_part}]: {description}".rstrip()
        )
        items.append(entry)
    return items


def module_help_text(module_def: Optional[Dict[str, Any]]) -> str:
    if not module_def:
        return ""
    sections: List[str] = []
    description = module_def.get("description", "")
    if description:
        sections.append(description)
    naming_rule = module_def.get("naming_rule")
    if naming_rule:
        sections.append(f"_Naming rule_: {naming_rule}")
    env_vars = module_def.get("env")
    if env_vars:
        env_text = ", ".join(env_vars)
        sections.append(f"Environment variables: `{env_text}`")
    granularity_levels = module_def.get("granularity_levels")
    if isinstance(granularity_levels, dict) and granularity_levels:
        lines: List[str] = []
        for level, text in sorted(
                granularity_levels.items(),
                key=lambda item: (0, int(item[0])) if str(item[0]).isdigit() else (1, str(item[0])),
        ):
            lines.append(f"- Level `{level}`: {text}")
        sections.append("Granularity levels:\n" + "\n".join(lines))
    arg_lines = describe_args(module_def.get("args", {}))
    if arg_lines:
        sections.append("Arguments:\n" + "\n".join(arg_lines))
    return "\n\n".join(sections)
