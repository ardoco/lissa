"""Reusable helpers for the config tool UI."""
from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any, Dict, Optional

import streamlit as st

from catalog import module_help_text

from constants import APP_ROOT

ConfigDict = Dict[str, Any]


def widget_epoch() -> int:
    return st.session_state.get("widget_epoch", 0)


def widget_key(base: str) -> str:
    return f"{base}::{widget_epoch()}"


def bump_widget_epoch() -> None:
    st.session_state["widget_epoch"] = widget_epoch() + 1


def load_json(path: Path) -> ConfigDict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def safe_copy(data: ConfigDict) -> ConfigDict:
    return copy.deepcopy(data)


def ensure_section(config: ConfigDict, path: list[str], default: Any) -> Any:
    node: Any = config
    for key in path[:-1]:
        if key not in node or not isinstance(node[key], dict):
            node[key] = {}
        node = node[key]
    leaf = path[-1]
    if leaf not in node:
        node[leaf] = copy.deepcopy(default)
    return node[leaf]


def value_to_text(value: Any) -> str:
    if isinstance(value, (dict, list)):
        return json.dumps(value)
    if isinstance(value, bool):
        return "true" if value else "false"
    return "" if value is None else str(value)


def parse_arg_value(raw_value: str) -> Any:
    cleaned = (raw_value or "").strip()
    if not cleaned:
        return ""
    if cleaned.startswith("{") or cleaned.startswith("["):
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError:
            return raw_value
    return raw_value


def render_args_editor(label: str, args: Dict[str, Any], key_prefix: str) -> Dict[str, Any]:
    rows = [{"Parameter": k, "Value": value_to_text(v)} for k, v in args.items()]
    if not rows:
        rows = [{"Parameter": "", "Value": ""}]
    edited_rows = st.data_editor(
        rows,
        num_rows="dynamic",
        key=widget_key(f"{key_prefix}-editor"),
        column_config={
            "Parameter": st.column_config.TextColumn("Parameter", width="medium"),
            "Value": st.column_config.TextColumn("Value", width="large"),
        },
        width="stretch",
        hide_index=True,
        disabled=False,
    )
    updated: Dict[str, Any] = {}
    for row in edited_rows:
        name = str(row.get("Parameter", "")).strip()
        if not name:
            continue
        updated[name] = parse_arg_value(str(row.get("Value", "")))
    return updated


def merge_dict(base: ConfigDict, overrides: ConfigDict) -> ConfigDict:
    for key, value in overrides.items():
        if isinstance(value, dict) and isinstance(base.get(key), dict):
            merge_dict(base[key], value)
        else:
            base[key] = value
    return base


def resolve_path(path_str: str) -> Path:
    candidate = Path(path_str).expanduser()
    if not candidate.is_absolute():
        candidate = (APP_ROOT / candidate).resolve()
    return candidate


def save_config_to_disk(config: ConfigDict, destination: str) -> Path:
    path = resolve_path(destination)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        json.dump(config, handle, indent=2)
        handle.write("\n")
    return path


def normalize_store_config(config: ConfigDict) -> list[str]:
    notes: list[str] = []

    target_store = config.get("target_store")
    if not isinstance(target_store, dict):
        config["target_store"] = {"name": "cosine_similarity", "args": {}}
        notes.append("target store set to cosine_similarity (missing section)")
    else:
        if target_store.get("name") == "custom":
            target_store["name"] = "cosine_similarity"
            notes.append("target store renamed from legacy 'custom' to 'cosine_similarity'")
        if "args" not in target_store or not isinstance(target_store["args"], dict):
            target_store["args"] = {}

    source_store = config.get("source_store")
    if not isinstance(source_store, dict):
        config["source_store"] = {"name": "custom", "args": {}}
        notes.append("source store reset to required 'custom' store")
    else:
        if source_store.get("name") != "custom":
            source_store["name"] = "custom"
            notes.append("source store forced to 'custom' (only supported option)")
        if source_store.get("args"):
            source_store["args"] = {}

    return notes


def module_name_input(
        *,
        label: str,
        key_prefix: str,
        module_config: ConfigDict,
        definitions: Dict[str, Dict[str, Any]],
) -> Optional[Dict[str, Any]]:
    current = module_config.get("name", "") or ""
    if not definitions:
        module_config["name"] = st.text_input(
            f"{label} name",
            value=current,
            key=widget_key(f"{key_prefix}-text"),
        )
        return None

    known = sorted(definitions.keys())
    options = ["<custom>"] + known
    has_known = current in known
    index = options.index(current) if has_known else 0
    selection = st.selectbox(
        f"{label} type",
        options=options,
        index=index,
        key=widget_key(f"{key_prefix}-selector"),
    )

    module_def: Optional[Dict[str, Any]] = None
    if selection == "<custom>":
        custom_default = "" if has_known else current
        module_config["name"] = st.text_input(
            f"{label} custom name",
            value=custom_default,
            key=widget_key(f"{key_prefix}-custom"),
        )
    else:
        module_config["name"] = selection
        module_def = definitions.get(selection)

    return module_def


def render_module_help(title: str, module_def: Optional[Dict[str, Any]], extra: Optional[str] = None) -> None:
    if not module_def and not extra:
        return
    parts = []
    if module_def:
        help_text = module_help_text(module_def)
        if help_text:
            parts.append(help_text)
    if extra:
        parts.append(extra)
    if parts:
        st.markdown("\n\n".join(parts))


def classifier_help(name: str, catalog: Dict[str, Any]) -> Optional[str]:
    if not name:
        return None
    mode_defs = catalog.get("classifier_modes", {})
    platform_defs = catalog.get("classifier_platforms", {})
    if name == "mock":
        mode_def = mode_defs.get("mock")
        platform_def = None
        platform_key = ""
    else:
        parts = name.split("_", 1)
        mode_key = parts[0] if parts else name
        platform_key = parts[1].upper() if len(parts) == 2 else ""
        mode_def = mode_defs.get(mode_key)
        platform_def = platform_defs.get(platform_key) if platform_key else None

    fragments = []
    if mode_def:
        fragments.append(module_help_text(mode_def))
    if platform_def:
        envs = platform_def.get("env", [])
        env_text = ", ".join(envs) if envs else "No required env vars listed."
        fragments.append(
            "Platform **{name}** -> default model `{model}`, threads={threads}. Required env: {envs}.".format(
                name=platform_key or "platform",
                model=platform_def.get("default_model", "n/a"),
                threads=platform_def.get("threads", "?"),
                envs=env_text,
            ))
    return "\n\n".join(fragment for fragment in fragments if fragment)
