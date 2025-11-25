"""Shared constants for the config tool."""
from __future__ import annotations

import os
from pathlib import Path

APP_ROOT = Path(__file__).resolve().parent
TEMPLATE_DIR = APP_ROOT / "templates"
SAVE_TO_DISK_ENABLED = os.environ.get("CONFIG_TOOL_ENABLE_SAVE", "").lower() in {"1", "true", "yes", "on"}
