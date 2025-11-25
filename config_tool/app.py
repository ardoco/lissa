"""Streamlit UI for authoring LiSSA configuration files."""
from __future__ import annotations

import copy
import json
import os
from pathlib import Path
from typing import Any, Dict, Optional

import streamlit as st

from catalog import (
    artifact_provider_defs,
    classifier_mode_defs,
    classifier_platform_defs,
    embedding_defs,
    module_help_text,
    postprocessor_defs,
    preprocessor_defs,
    result_aggregator_defs,
    store_defs,
)

APP_ROOT = Path(__file__).resolve().parent
TEMPLATE_DIR = APP_ROOT / "templates"

SAVE_TO_DISK_ENABLED = os.environ.get("CONFIG_TOOL_ENABLE_SAVE", "").lower() in {"1", "true", "yes", "on"}

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
    # Fallback skeleton (should rarely be used but keeps the UI functional)
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
    """Apply current store defaults and return human-friendly notes about changes."""

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


def classifier_help(name: str) -> Optional[str]:
    if not name:
        return None
    if name == "mock":
        mode_def = CATALOG.get("classifier_modes", {}).get("mock")
        platform_def = None
        platform_key = ""
    else:
        parts = name.split("_", 1)
        mode_key = parts[0] if parts else name
        platform_key = parts[1].upper() if len(parts) == 2 else ""
        mode_def = CATALOG.get("classifier_modes", {}).get(mode_key)
        platform_def = CATALOG.get("classifier_platforms", {}).get(platform_key) if platform_key else None

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


st.set_page_config(page_title="LiSSA Config Tool", layout="wide")

if "config_data" not in st.session_state:
    st.session_state["config_data"] = initial_config()
if "template_name" not in st.session_state:
    st.session_state["template_name"] = default_template_label()
if "widget_epoch" not in st.session_state:
    st.session_state["widget_epoch"] = 0

st.title("LiSSA Configuration Tool")
st.caption("Interactive editor for doc2code, req2code, and req2req configuration files.")
if CATALOG.get("error"):
    st.warning(
        "Module catalog missing: %s. Falling back to manual text inputs."
        % CATALOG["error"],
    )

with st.sidebar:
    st.header("Templates & Imports")
    template_options = available_templates()
    template_keys = list(template_options.keys()) or ["<no templates found>"]
    default_index = template_keys.index(st.session_state["template_name"]) if st.session_state["template_name"] in template_keys else 0
    selected_template = st.selectbox(
        "Base template",
        options=template_keys,
        index=default_index,
    )
    if template_options:
        if st.button("Load template", width="stretch"):
            try:
                st.session_state["config_data"] = load_json(template_options[selected_template])
                st.session_state["template_name"] = selected_template
                bump_widget_epoch()
                st.success(f"Loaded {selected_template}")
            except Exception as exc:  # pragma: no cover - interactive feedback
                st.error(f"Could not load template: {exc}")
    else:
        st.warning("No template files detected.")

    uploaded_config = st.file_uploader("Open JSON configuration", type=["json"], key="open-config-uploader")
    if uploaded_config is not None:
        try:
            st.session_state["config_data"] = json.load(uploaded_config)
            bump_widget_epoch()
            st.success(f"Loaded {uploaded_config.name}")
        except json.JSONDecodeError as exc:
            st.error(f"Invalid JSON: {exc}")

    if st.button("Reset to defaults", width="stretch"):
        st.session_state["config_data"] = initial_config()
        st.session_state["template_name"] = default_template_label()
        bump_widget_epoch()
        st.success("Reset complete")

    st.markdown("---")
    st.subheader("Quick actions")
    st.markdown(
        "- Use the forms to edit the structure.\n"
        "- Supply JSON in any args field to nest complex settings.\n"
        "- Download the JSON preview or enable saving via CONFIG_TOOL_ENABLE_SAVE."
    )

config = st.session_state["config_data"]
store_normalization_notes = normalize_store_config(config)
if store_normalization_notes:
    bullets = "\n- ".join(store_normalization_notes)
    st.info("Store configuration normalized:\n- %s" % bullets)

st.subheader("1. General")
st.session_state["config_data"]["cache_dir"] = st.text_input(
    "Cache directory",
    value=config.get("cache_dir", ""),
)

gold_config = ensure_section(config, ["gold_standard_configuration"], {"path": "", "hasHeader": "false"})
col1, col2 = st.columns(2)
with col1:
    gold_config["path"] = st.text_input(
        "Gold standard CSV",
        value=gold_config.get("path", ""),
    )
with col2:
    has_header = str(gold_config.get("hasHeader", "false")).lower()
    gold_config["hasHeader"] = st.selectbox(
        "Gold standard has header?",
        options=["true", "false"],
        index=0 if has_header == "true" else 1,
    )

st.divider()
st.subheader("2. Artifact providers")
provider_cols = st.columns(2)
for idx, (label, key_name) in enumerate(
    (("Source", "source_artifact_provider"), ("Target", "target_artifact_provider")),
):
    with provider_cols[idx]:
        provider = ensure_section(config, [key_name], {"name": "text", "args": {}})
        provider_def = module_name_input(
            label=f"{label} provider",
            key_prefix=f"{label.lower()}-provider",
            module_config=provider,
            definitions=CATALOG.get("artifact_providers", {}),
        )
        render_module_help(f"{label} provider", provider_def)
        provider_args = ensure_section(provider, ["args"], {})
        provider["args"] = render_args_editor(f"{label} provider args", provider_args, key_prefix=f"{label}-provider")

st.divider()
st.subheader("3. Preprocessors")
pre_cols = st.columns(2)
for idx, (label, key_name) in enumerate(
    (("Source", "source_preprocessor"), ("Target", "target_preprocessor")),
):
    with pre_cols[idx]:
        pre = ensure_section(config, [key_name], {"name": "artifact", "args": {}})
        pre_def = module_name_input(
            label=f"{label} preprocessor",
            key_prefix=f"{label.lower()}-pre",
            module_config=pre,
            definitions=CATALOG.get("preprocessors", {}),
        )
        render_module_help(f"{label} preprocessor", pre_def)
        pre_args = ensure_section(pre, ["args"], {})
        pre["args"] = render_args_editor(f"{label} preprocessor args", pre_args, key_prefix=f"{label}-pre")

st.divider()
st.subheader("4. Embeddings & stores")
emb = ensure_section(config, ["embedding_creator"], {"name": "openai", "args": {}})
emb_def = module_name_input(
    label="Embedding creator",
    key_prefix="embedding",
    module_config=emb,
    definitions=CATALOG.get("embedding_creators", {}),
)
render_module_help("Embedding creator", emb_def)
emb_args = ensure_section(emb, ["args"], {})
emb["args"] = render_args_editor("Embedding args", emb_args, key_prefix="embedding")

store_cols = st.columns(2)
with store_cols[0]:
    source_store = ensure_section(config, ["source_store"], {"name": "custom", "args": {}})
    source_store["name"] = "custom"
    source_store["args"] = {}
    st.markdown("**Source store**")
    st.caption("Fixed to the plain element store (no similarity search); nothing to configure here.")

with store_cols[1]:
    target_store = ensure_section(config, ["target_store"], {"name": "cosine_similarity", "args": {}})
    target_store_def = module_name_input(
        label="Target store",
        key_prefix="target-store",
        module_config=target_store,
        definitions=CATALOG.get("target_store", {}),
    )
    render_module_help("Target store", target_store_def)
    target_store_args = ensure_section(target_store, ["args"], {})
    target_store["args"] = render_args_editor("Target store args", target_store_args, key_prefix="target-store-args")

st.divider()
st.subheader("5. Classifier & aggregation")
multi_stage_classifier = config.get("classifiers")
if isinstance(multi_stage_classifier, list):
    st.info(
        "Detected a multi-stage classifier pipeline. The current UI is read-only for this mode; "
        "edit the JSON below or convert it to a single `classifier` entry.",
    )
    st.code(json.dumps(multi_stage_classifier, indent=2), language="json")
    st.caption("Schema details: see `classifier_modes`, `classifier_platforms`, and multi-stage rules in module_catalog.json.")
else:
    classifier = ensure_section(config, ["classifier"], {"name": "reasoning_openai", "args": {}})
    mode_defs = CATALOG.get("classifier_modes", {})
    platform_defs = CATALOG.get("classifier_platforms", {})
    current_name = classifier.get("name", "") or ""
    if current_name == "mock":
        current_mode = "mock"
        current_platform = ""
    elif "_" in current_name:
        current_mode, current_platform = current_name.split("_", 1)
    else:
        current_mode = current_name
        current_platform = ""

    mode_options = ["<custom>"] + sorted(mode_defs.keys())
    mode_index = mode_options.index(current_mode) if current_mode in mode_defs else 0
    selected_mode = st.selectbox(
        "Classifier mode",
        options=mode_options,
        index=mode_index,
        key=widget_key("classifier-mode-selector"),
    )
    if selected_mode == "<custom>":
        mode_value = st.text_input(
            "Custom mode name",
            value="" if current_mode in mode_defs else current_mode,
            key=widget_key("classifier-mode-custom"),
        ).strip()
    else:
        mode_value = selected_mode
    mode_def = mode_defs.get(mode_value)

    platform_options = ["<none>", "<custom>"] + sorted(platform_defs.keys())
    platform_disabled = mode_value == "mock"
    platform_index = 0
    upper_platform = current_platform.upper()
    if current_platform and upper_platform in platform_defs:
        platform_index = platform_options.index(upper_platform)
    elif current_platform and current_platform not in ("<none>", ""):
        platform_index = platform_options.index("<custom>")
    platform_selection = st.selectbox(
        "Classifier platform",
        options=platform_options,
        index=platform_index,
        key=widget_key("classifier-platform-selector"),
        disabled=platform_disabled,
    )
    if platform_disabled:
        platform_value = ""
    elif platform_selection == "<none>":
        platform_value = ""
    elif platform_selection == "<custom>":
        platform_value = st.text_input(
            "Custom platform name",
            value=current_platform if upper_platform not in platform_defs else "",
            key=widget_key("classifier-platform-custom"),
        ).strip()
    else:
        platform_value = platform_selection.lower()
    platform_def = None
    if platform_value:
        platform_def = platform_defs.get(platform_value.upper())

    if mode_value:
        classifier["name"] = mode_value if not platform_value else f"{mode_value}_{platform_value}"
    else:
        classifier["name"] = ""

    render_module_help("Classifier", mode_def, extra=classifier_help(classifier.get("name", "")))
    classifier_args = ensure_section(classifier, ["args"], {})
    classifier["args"] = render_args_editor("Classifier args", classifier_args, key_prefix="classifier")

result_agg = ensure_section(config, ["result_aggregator"], {"name": "any_connection", "args": {}})
result_agg_def = module_name_input(
    label="Result aggregator",
    key_prefix="result-aggregator",
    module_config=result_agg,
    definitions=CATALOG.get("result_aggregators", {}),
)
render_module_help("Result aggregator", result_agg_def)
result_agg_args = ensure_section(result_agg, ["args"], {})
result_agg["args"] = render_args_editor("Aggregator args", result_agg_args, key_prefix="result-agg")

post = ensure_section(config, ["tracelinkid_postprocessor"], {"name": "identity", "args": {}})
post_def = module_name_input(
    label="Trace link post-processor",
    key_prefix="postprocessor",
    module_config=post,
    definitions=CATALOG.get("postprocessors", {}),
)
render_module_help("Trace link post-processor", post_def)
post_args = ensure_section(post, ["args"], {})
post["args"] = render_args_editor("Post-processor args", post_args, key_prefix="post")

st.divider()
st.subheader("6. Raw JSON overrides (optional)")
st.caption("Paste any JSON snippet to merge it into the current configuration.")
override_text = st.text_area(
    "Override payload",
    value=st.session_state.get("raw_override_text", ""),
    height=150,
    key="raw_override_text",
)
if st.button("Apply overrides"):
    try:
        overrides = json.loads(st.session_state.get("raw_override_text", ""))
        if not isinstance(overrides, dict):
            raise ValueError("Overrides must be a JSON object")
        merge_dict(config, overrides)
        st.success("Overrides applied")
    except Exception as exc:  # pragma: no cover - interactive feedback
        st.error(f"Could not apply overrides: {exc}")

st.divider()
st.subheader("7. Preview & export")
preview = json.dumps(config, indent=2)
st.code(preview, language="json")

st.text_input(
    "Destination path",
    value=st.session_state.get("output_path", "output/config.json"),
    key="output_path",
)

col_save, col_download = st.columns(2)
with col_save:
    if SAVE_TO_DISK_ENABLED:
        if st.button("Save to disk", width="stretch"):
            try:
                saved_path = save_config_to_disk(config, st.session_state["output_path"])
                try:
                    display_path = saved_path.relative_to(APP_ROOT)
                except ValueError:
                    display_path = saved_path
                st.success(f"Saved to {display_path}")
            except Exception as exc:  # pragma: no cover - interactive feedback
                st.error(f"Failed to save file: {exc}")
    else:
        st.button("Save to disk", width="stretch", disabled=True)
        st.caption("Set CONFIG_TOOL_ENABLE_SAVE=1 to enable saving inside the container.")
with col_download:
    filename = Path(st.session_state["output_path"]).name or "config.json"
    st.download_button(
        "Download JSON",
        data=preview,
        file_name=filename,
        mime="application/json",
        width="stretch",
    )

st.info("Tip: use the download button to export configurations if saving is disabled.")
