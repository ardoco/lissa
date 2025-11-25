"""UI sections for the Streamlit config tool."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Optional

import streamlit as st

from constants import APP_ROOT
from helpers import (
    ConfigDict,
    classifier_help,
    ensure_section,
    merge_dict,
    module_help_markdown,
    module_name_input,
    render_args_editor,
    render_module_help,
    save_config_to_disk,
    widget_key,
)


def render_general_section(config: ConfigDict) -> None:
    st.subheader("1. General")
    st.session_state["config_data"]["cache_dir"] = st.text_input(
        "Cache directory",
        value=config.get("cache_dir", ""),
    )

    gold_config = ensure_section(
        config, ["gold_standard_configuration"], {"path": "", "hasHeader": "false"}
    )
    col1, col2 = st.columns(2, vertical_alignment="top")
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


def render_artifact_provider_section(
    config: ConfigDict, catalog: Dict[str, Any]
) -> None:
    st.divider()
    st.subheader("2. Artifact providers")
    provider_cols = st.columns(2, vertical_alignment="top")
    help_blocks: list[Optional[str]] = [None, None]
    for idx, (label, key_name) in enumerate(
        (
            ("Source", "source_artifact_provider"),
            ("Target", "target_artifact_provider"),
        ),
    ):
        with provider_cols[idx]:
            provider = ensure_section(config, [key_name], {"name": "text", "args": {}})
            provider_def = module_name_input(
                label=f"{label} provider",
                key_prefix=f"{label.lower()}-provider",
                module_config=provider,
                definitions=catalog.get("artifact_providers", {}),
            )
            provider_args = ensure_section(provider, ["args"], {})
            provider["args"] = render_args_editor(
                f"{label} provider args", provider_args, key_prefix=f"{label}-provider"
            )
            help_blocks[idx] = module_help_markdown(provider_def)

    if any(help_blocks):
        help_cols = st.columns(2, vertical_alignment="top")
        for idx, help_md in enumerate(help_blocks):
            if help_md:
                with help_cols[idx]:
                    st.markdown(help_md)


def render_preprocessor_section(config: ConfigDict, catalog: Dict[str, Any]) -> None:
    st.divider()
    st.subheader("3. Preprocessors")
    pre_cols = st.columns(2, vertical_alignment="top")
    help_blocks: list[Optional[str]] = [None, None]
    for idx, (label, key_name) in enumerate(
        (("Source", "source_preprocessor"), ("Target", "target_preprocessor")),
    ):
        with pre_cols[idx]:
            pre = ensure_section(config, [key_name], {"name": "artifact", "args": {}})
            pre_def = module_name_input(
                label=f"{label} preprocessor",
                key_prefix=f"{label.lower()}-pre",
                module_config=pre,
                definitions=catalog.get("preprocessors", {}),
            )
            pre_args = ensure_section(pre, ["args"], {})
            pre["args"] = render_args_editor(
                f"{label} preprocessor args", pre_args, key_prefix=f"{label}-pre"
            )
            help_blocks[idx] = module_help_markdown(pre_def)

    if any(help_blocks):
        help_cols = st.columns(2, vertical_alignment="top")
        for idx, help_md in enumerate(help_blocks):
            if help_md:
                with help_cols[idx]:
                    st.markdown(help_md)


def render_embedding_section(config: ConfigDict, catalog: Dict[str, Any]) -> None:
    st.divider()
    st.subheader("4. Embeddings")
    emb = ensure_section(config, ["embedding_creator"], {"name": "openai", "args": {}})
    emb_def = module_name_input(
        label="Embedding creator",
        key_prefix="embedding",
        module_config=emb,
        definitions=catalog.get("embedding_creators", {}),
    )
    emb_args = ensure_section(emb, ["args"], {})
    emb["args"] = render_args_editor("Embedding args", emb_args, key_prefix="embedding")
    render_module_help("Embedding creator", emb_def)


def render_store_section(config: ConfigDict, catalog: Dict[str, Any]) -> None:
    st.divider()
    st.subheader("5. Stores")
    store_cols = st.columns(2, vertical_alignment="top")
    with store_cols[0]:
        source_store = ensure_section(
            config, ["source_store"], {"name": "custom", "args": {}}
        )
        source_store["name"] = "custom"
        source_store["args"] = {}
        st.markdown("**Source store**")
        st.caption(
            "Fixed to the plain element store (no similarity search); nothing to configure here."
        )

    with store_cols[1]:
        st.markdown("**Target store**")
        target_store = ensure_section(
            config, ["target_store"], {"name": "cosine_similarity", "args": {}}
        )
        target_store_def = module_name_input(
            label="Target store",
            key_prefix="target-store",
            module_config=target_store,
            definitions=catalog.get("target_store", {}),
        )
        target_store_args = ensure_section(target_store, ["args"], {})
        target_store["args"] = render_args_editor(
            "Target store args", target_store_args, key_prefix="target-store-args"
        )
        render_module_help("Target store", target_store_def)


def render_classifier_and_aggregation_section(
    config: ConfigDict, catalog: Dict[str, Any]
) -> None:
    st.divider()
    st.subheader("6. Classifier & aggregation")
    multi_stage_classifier = config.get("classifiers")
    if isinstance(multi_stage_classifier, list):
        st.info(
            "Detected a multi-stage classifier pipeline. The current UI is read-only for this mode; "
            "edit the JSON below or convert it to a single `classifier` entry.",
        )
        st.code(json.dumps(multi_stage_classifier, indent=2), language="json")
        st.caption(
            "Schema details: see `classifier_modes`, `classifier_platforms`, and multi-stage rules in module_catalog.json."
        )
    else:
        classifier = ensure_section(
            config, ["classifier"], {"name": "reasoning_openai", "args": {}}
        )
        mode_defs = catalog.get("classifier_modes", {})
        platform_defs = catalog.get("classifier_platforms", {})
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
        mode_index = (
            mode_options.index(current_mode) if current_mode in mode_defs else 0
        )
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
            classifier["name"] = (
                mode_value if not platform_value else f"{mode_value}_{platform_value}"
            )
        else:
            classifier["name"] = ""

        classifier_args = ensure_section(classifier, ["args"], {})
        classifier["args"] = render_args_editor(
            "Classifier args", classifier_args, key_prefix="classifier"
        )
        render_module_help(
            "Classifier",
            mode_def,
            extra=classifier_help(classifier.get("name", ""), catalog),
        )

    result_agg = ensure_section(
        config, ["result_aggregator"], {"name": "any_connection", "args": {}}
    )
    result_agg_def = module_name_input(
        label="Result aggregator",
        key_prefix="result-aggregator",
        module_config=result_agg,
        definitions=catalog.get("result_aggregators", {}),
    )
    result_agg_args = ensure_section(result_agg, ["args"], {})
    result_agg["args"] = render_args_editor(
        "Aggregator args", result_agg_args, key_prefix="result-agg"
    )
    render_module_help("Result aggregator", result_agg_def)

    post = ensure_section(
        config, ["tracelinkid_postprocessor"], {"name": "identity", "args": {}}
    )
    post_def = module_name_input(
        label="Trace link post-processor",
        key_prefix="postprocessor",
        module_config=post,
        definitions=catalog.get("postprocessors", {}),
    )
    post_args = ensure_section(post, ["args"], {})
    post["args"] = render_args_editor(
        "Post-processor args", post_args, key_prefix="post"
    )
    render_module_help("Trace link post-processor", post_def)


def render_overrides_section(config: ConfigDict) -> None:
    st.divider()
    st.subheader("7. Raw JSON overrides (optional)")
    st.caption("Paste any JSON snippet to merge it into the current configuration.")
    override_text = st.text_area(
        "Override payload",
        value=st.session_state.get("raw_override_text", ""),
        height=150,
        key="raw_override_text",
    )
    if st.button("Apply overrides"):
        try:
            overrides = json.loads(override_text or "")
            if not isinstance(overrides, dict):
                raise ValueError("Overrides must be a JSON object")
            merge_dict(config, overrides)
            st.success("Overrides applied")
        except Exception as exc:  # pragma: no cover - interactive feedback
            st.error(f"Could not apply overrides: {exc}")


def render_preview_and_export(config: ConfigDict, save_enabled: bool) -> None:
    st.divider()
    st.subheader("8. Preview & export")
    preview = json.dumps(config, indent=2)
    st.code(preview, language="json")

    st.text_input(
        "Destination path",
        key="output_path",
        disabled=not save_enabled,
    )
    if not save_enabled:
        st.caption(
            "Destination path editing is disabled because saving to disk is turned off."
        )

    col_save, col_download = st.columns(2, vertical_alignment="top")
    with col_save:
        if save_enabled:
            if st.button("Save to disk", width="stretch"):
                try:
                    saved_path = save_config_to_disk(
                        config, st.session_state["output_path"]
                    )
                    try:
                        display_path = saved_path.relative_to(APP_ROOT)
                    except ValueError:
                        display_path = saved_path
                    st.success(f"Saved to {display_path}")
                except Exception as exc:  # pragma: no cover - interactive feedback
                    st.error(f"Failed to save file: {exc}")
        else:
            st.button("Save to disk", width="stretch", disabled=True)
            st.caption(
                "Set CONFIG_TOOL_ENABLE_SAVE=1 to enable saving inside the container."
            )
    with col_download:
        filename = Path(st.session_state["output_path"]).name or "config.json"
        st.download_button(
            "Download JSON",
            data=preview,
            file_name=filename,
            mime="application/json",
            width="stretch",
            disabled=False,
        )

    st.info(
        "Tip: use the download button to export configurations if saving is disabled."
    )
