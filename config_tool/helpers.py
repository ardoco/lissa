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


def _rows_state_key(key_prefix: str) -> str:
    return f"{key_prefix}-args-rows"


def _rows_epoch_key(key_prefix: str) -> str:
    return f"{key_prefix}-args-rows-epoch"


def _rows_counter_key(key_prefix: str) -> str:
    return f"{key_prefix}-args-rows-counter"


def render_args_editor(label: str, args: Dict[str, Any], key_prefix: str) -> Dict[str, Any]:
    state_key = _rows_state_key(key_prefix)
    epoch_key = _rows_epoch_key(key_prefix)
    counter_key = _rows_counter_key(key_prefix)
    current_epoch = widget_epoch()
    if st.session_state.get(epoch_key) != current_epoch or state_key not in st.session_state:
        st.session_state[state_key] = [
            {"id": idx, "parameter": k, "value": value_to_text(v)}
            for idx, (k, v) in enumerate(args.items())
        ]
        st.session_state[epoch_key] = current_epoch
        st.session_state[counter_key] = len(st.session_state[state_key])
    elif counter_key not in st.session_state:
        st.session_state[counter_key] = len(st.session_state[state_key])

    rows: list[Dict[str, str]] = copy.deepcopy(st.session_state[state_key])

    def _ensure_row_ids() -> None:
        next_id = st.session_state.get(counter_key, 0)
        updated = False
        for row in rows:
            if "id" not in row:
                row["id"] = next_id
                next_id += 1
                updated = True
        if updated:
            st.session_state[counter_key] = next_id
            st.session_state[state_key] = copy.deepcopy(rows)

    _ensure_row_ids()

    st.markdown(f"**{label}**")
    header_cols = st.columns([3, 3, 0.8], vertical_alignment="center")
    header_cols[0].caption("Parameter")
    header_cols[1].caption("Value")
    with header_cols[2]:
        if st.button(
                "＋",
                key=widget_key(f"{key_prefix}-add-row"),
                use_container_width=True,
                help="Add parameter",
                type="secondary",
        ):
            next_id = st.session_state.get(counter_key, 0)
            rows.append({"id": next_id, "parameter": "", "value": ""})
            st.session_state[counter_key] = next_id + 1

    def _commit_rows(updated_rows: list[Dict[str, str]]) -> None:
        """Persist the current rows and refresh UI when needed."""
        st.session_state[state_key] = copy.deepcopy(updated_rows)

    def _trigger_rerun() -> None:
        rerun = getattr(st, "rerun", None)
        if rerun is not None:
            rerun()
        else:  # pragma: no cover - fallback for older Streamlit versions
            st.experimental_rerun()

    def _remove_row(row_id: int) -> None:
        remaining = [row for row in rows if row.get("id") != row_id]
        if len(remaining) != len(rows):
            rows[:] = remaining
            _commit_rows(rows)
            _trigger_rerun()

    for idx, row in enumerate(rows):
        param_col, value_col, remove_col = st.columns([3, 3, 0.8], vertical_alignment="center")
        row_id = row.get("id", idx)
        param_key = widget_key(f"{key_prefix}-row-{row_id}-param")
        value_key = widget_key(f"{key_prefix}-row-{row_id}-value")
        with param_col:
            param_value = st.text_input(
                "Parameter",
                value=row.get("parameter", ""),
                key=param_key,
                label_visibility="collapsed",
            )
        with value_col:
            value_value = st.text_input(
                "Value",
                value=row.get("value", ""),
                key=value_key,
                label_visibility="collapsed",
            )
        with remove_col:
            st.button(
                "－",
                key=widget_key(f"{key_prefix}-row-{row_id}-remove"),
                help="Remove this parameter",
                type="secondary",
                use_container_width=True,
                on_click=_remove_row,
                args=(row_id,),
            )
        row["parameter"] = param_value
        row["value"] = value_value

    _commit_rows(rows)
    updated: Dict[str, Any] = {}
    for row in rows:
        name = row.get("parameter", "").strip()
        if not name:
            continue
        updated[name] = parse_arg_value(row.get("value", ""))
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


def module_help_markdown(module_def: Optional[Dict[str, Any]], extra: Optional[str] = None) -> Optional[str]:
    """Build the markdown snippet that explains a selected module."""
    if not module_def and not extra:
        return None
    parts = []
    if module_def:
        help_text = module_help_text(module_def)
        if help_text:
            parts.append(help_text)
    if extra:
        parts.append(extra)
    if parts:
        return "\n\n".join(parts)
    return None


def render_module_help(title: str, module_def: Optional[Dict[str, Any]], extra: Optional[str] = None) -> None:
    help_text = module_help_markdown(module_def, extra)
    if help_text:
        st.markdown(help_text)


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

    if platform_def:
        envs = platform_def.get("env", [])
        env_text = ", ".join(envs) if envs else "No required env vars listed."
        return (
            "Platform **{name}** -> default model `{model}`, threads={threads}. Required env: {envs}.".format(
                name=platform_key or "platform",
                model=platform_def.get("default_model", "n/a"),
                threads=platform_def.get("threads", "?"),
                envs=env_text,
            )
        )
    return None
