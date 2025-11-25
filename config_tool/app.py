"""Streamlit UI for authoring LiSSA configuration files."""
from __future__ import annotations

import json

import streamlit as st

from catalog_loader import CATALOG
from constants import SAVE_TO_DISK_ENABLED
from helpers import bump_widget_epoch, load_json, normalize_store_config
from template_utils import available_templates, default_template_label, initial_config
from ui_sections import (
    render_artifact_provider_section,
    render_classifier_and_aggregation_section,
    render_embedding_and_store_section,
    render_general_section,
    render_overrides_section,
    render_preprocessor_section,
    render_preview_and_export,
)


st.set_page_config(page_title="LiSSA Config Tool", layout="wide")


def init_session() -> None:
    if "config_data" not in st.session_state:
        st.session_state["config_data"] = initial_config()
    if "template_name" not in st.session_state:
        st.session_state["template_name"] = default_template_label()
    if "widget_epoch" not in st.session_state:
        st.session_state["widget_epoch"] = 0
    if "output_path" not in st.session_state:
        st.session_state["output_path"] = "output/config.json"
    if "raw_override_text" not in st.session_state:
        st.session_state["raw_override_text"] = ""


def render_sidebar() -> None:
    with st.sidebar:
        st.header("Templates & Imports")
        template_options = available_templates()
        template_keys = list(template_options.keys()) or ["<no templates found>"]
        selected_name = st.session_state.get("template_name", template_keys[0])
        default_index = template_keys.index(selected_name) if selected_name in template_keys else 0
        selected_template = st.selectbox(
            "Base template",
            options=template_keys,
            index=default_index,
        )

        if template_options and st.button("Load template", use_container_width=True):
            try:
                st.session_state["config_data"] = load_json(template_options[selected_template])
                st.session_state["template_name"] = selected_template
                bump_widget_epoch()
                st.success(f"Loaded {selected_template}")
            except Exception as exc:  # pragma: no cover - interactive feedback
                st.error(f"Could not load template: {exc}")
        elif not template_options:
            st.warning("No template files detected.")

        uploaded_config = st.file_uploader("Open JSON configuration", type=["json"], key="open-config-uploader")
        if uploaded_config is not None:
            try:
                st.session_state["config_data"] = json.load(uploaded_config)
                st.session_state["template_name"] = "<uploaded>"
                bump_widget_epoch()
                st.success(f"Loaded {uploaded_config.name}")
            except json.JSONDecodeError as exc:
                st.error(f"Invalid JSON: {exc}")

        if st.button("Reset to defaults", use_container_width=True):
            st.session_state["config_data"] = initial_config()
            st.session_state["template_name"] = default_template_label()
            bump_widget_epoch()
            st.success("Reset complete")

        st.markdown("---")
        st.subheader("Quick tips")
        st.markdown(
            "- Args tables accept plain text or JSON snippets.\n"
            "- Paths may be absolute or relative to the repo root.\n"
            "- Enable saving inside Docker via `CONFIG_TOOL_ENABLE_SAVE=1`."
        )


def main() -> None:
    init_session()

    st.title("LiSSA Configuration Tool")
    st.caption("Interactive editor for doc2code, req2code, and req2req configuration files.")
    if CATALOG.get("error"):
        st.warning(
            "Module catalog missing: %s. Falling back to text inputs."
            % CATALOG["error"],
        )

    render_sidebar()

    config = st.session_state["config_data"]
    store_notes = normalize_store_config(config)
    if store_notes:
        bullets = "\n- ".join(store_notes)
        st.info("Store configuration normalized:\n- %s" % bullets)

    render_general_section(config)
    render_artifact_provider_section(config, CATALOG)
    render_preprocessor_section(config, CATALOG)
    render_embedding_and_store_section(config, CATALOG)
    render_classifier_and_aggregation_section(config, CATALOG)
    render_overrides_section(config)
    render_preview_and_export(config, SAVE_TO_DISK_ENABLED)


if __name__ == "__main__":
    main()
