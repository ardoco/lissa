# Config Tool

A lightweight Streamlit application to create and edit LiSSA configuration files with a graphical interface. The tool wraps the structure that is currently generated via the Python helpers (e.g., `generate_configs_*`) and exposes the most relevant fields as form elements.

## Features

- Start from the repository-wide `config-template.json` or any template located in `config_tool/templates`.
- Import existing JSON configurations (drag & drop) and continue editing them visually.
- Guided sections for general metadata, artifact providers, preprocessors, embedding + store settings, classifier, and post-processing.
- Key/value editor for every `args` block with support for nested JSON snippets.
- Live JSON preview, download button, and one-click save into the `configs/` directory (or any custom path).

## Getting Started

1. **Install dependencies** (ideally inside a virtual environment):
   ```bash
   pip install -r config_tool/requirements.txt
   ```
2. **Launch the UI** from the repository root:
   ```bash
   streamlit run config_tool/app.py
   ```
3. Use the sidebar to pick a base template or import an existing configuration. Update the form fields, preview the JSON, and either download it or save it into the repository.

Whenever you need to check which module names and arguments the backend actually supports, consult `config_tool/module_catalog.json`. It is generated from the Java implementation and mirrors the available artifact providers, preprocessors, embedding creators, stores, classifiers, aggregators, and trace link post-processors.

## Catalog maintenance

The Streamlit widgets read all dropdown options, descriptions, argument hints, and environment requirements from `module_catalog.json`. Keep it in sync with the Java backend whenever module classes change:

1. Inspect the relevant packages under `edu.kit.kastel.sdq.lissa.ratlr` (artifact providers, preprocessors, embedding creators, stores, classifiers, aggregators, post-processors).
2. Add or update the corresponding entry inside `module_catalog.json`, including `name`, `class`, `description`, `args`, and optional `env` / `naming_rule` metadata. You can reference other catalog data via `"@pointer"` strings (e.g., `"@artifact_types"`).
3. Bump the `last_reviewed` field so everyone knows when the catalog was last aligned with the Java code.
4. Restart `streamlit run config_tool/app.py` to pick up the new metadata; the UI will automatically surface the updated options.

Thanks to this catalog-driven approach, users always see authoritative module names, parameter descriptions, allowed enum values, and required environment variables while editing configurations.

## Tips

- Every `args` table accepts plain strings or JSON snippets (e.g., `{ "chunk_size": "200" }`). Values are kept as strings unless a valid JSON fragment is provided.
- Paths may be absolute or relative to the repository root. The saver will create missing parent directories automatically.
- If you need advanced tweaks that are not part of the guided form, use the "Raw JSON overrides" text area at the bottom to patch arbitrary parts of the configuration before exporting.
- The target store now uses the `cosine_similarity` retrieval strategy; legacy configs that still specify `custom` are upgraded automatically when opened in the UI.
