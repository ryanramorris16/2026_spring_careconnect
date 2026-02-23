# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/normalize.py
# ==========================================================
# normalize.py
# ----------------------------------------------------------
# Normalization Layer
#
# This module is Layer 1 of the Quality Gate subsystem.
#
# Purpose:
#   Convert heterogeneous tool outputs (XML/JSON/JSONL) into a
#   single consistent schema so policy evaluation can be simple
#   and deterministic.
#
# Inputs:
#   Raw tool artifacts are expected in:
#     quality/analysis/raw/
#
# Output:
#   A single normalized file is written to:
#     quality/analysis/normalized/normalized.json
#
# IMPORTANT DESIGN RULES:
# - This layer DOES NOT apply policy thresholds.
# - This layer DOES NOT decide pass/fail.
# - This layer MUST be resilient: one parser crashing should not
#   prevent other tool results from being collected.
# - Any parser crash becomes a runtime_error for that tool, which
#   will later block the merge per governance requirements.
# ==========================================================

from __future__ import annotations

import json
from pathlib import Path
from typing import Callable, Dict, List, Tuple

# ----------------------------------------------------------
# Standard schema factory used when parsers fail or artifacts
# are missing.
#
# NOTE:
# - We use relative imports so this works when executed as:
#     python -m quality.ci.gate.normalize
#   (recommended)
# - Direct execution still works from repo root because Python
#   sets the working directory on sys.path; but module execution
#   is more reliable.
# ----------------------------------------------------------
from .schemas import base_tool_result

# ----------------------------------------------------------
# Tool parsers
# ----------------------------------------------------------
# Each parser is responsible for reading a tool-specific raw artifact
# from quality/analysis/raw/ and returning a standardized result dict.
#
# NOTE: Parsers must NOT implement enforcement policy. They only
# extract and normalize findings + severity information.
# ----------------------------------------------------------
from .parsers.trufflehog import parse_trufflehog
from .parsers.flutter import parse_flutter
from .parsers.checkstyle import parse_checkstyle
from .parsers.pmd import parse_pmd
from .parsers.spotbugs import parse_spotbugs
from .parsers.semgrep import parse_semgrep
from .parsers.dependency_check import parse_dependency_check
from .parsers.sonar import parse_sonar

# ----------------------------------------------------------
# Directory configuration
# ----------------------------------------------------------
# RAW_DIR:
#   Location where the CI workflow must write raw tool reports.
#
# NORMALIZED_DIR:
#   Output directory for normalized.json.
# ----------------------------------------------------------
RAW_DIR = Path("quality/analysis/raw")
NORMALIZED_DIR = Path("quality/analysis/normalized")
NORMALIZED_DIR.mkdir(parents=True, exist_ok=True)

# Single normalized output file used by policy_engine.py
OUTPUT_FILE = NORMALIZED_DIR / "normalized.json"

# ----------------------------------------------------------
# Tool registration list
# ----------------------------------------------------------
# Order here is mostly for readability in the final report.
# Each entry is:
#   (tool_key, parser_function)
#
# tool_key MUST match the key used in:
#   - policy.yaml (tools.<tool_key>)
#   - report output
# ----------------------------------------------------------
ToolParser = Callable[[Path], Dict]
TOOL_PARSERS: List[Tuple[str, ToolParser]] = [
    ("trufflehog", parse_trufflehog),  # Secrets scan (JSONL)
    ("flutter_analyze", parse_flutter),  # Flutter/Dart analysis (JSON)
    ("checkstyle", parse_checkstyle),  # Java style checks (XML)
    ("pmd", parse_pmd),  # Java source analysis (XML)
    ("spotbugs", parse_spotbugs),  # Java bytecode analysis (XML)
    ("semgrep", parse_semgrep),  # Multi-language SAST (JSON)
    ("dependency_check", parse_dependency_check),  # SCA (JSON)
    ("sonar", parse_sonar),  # Quality Gate (JSON)
]


def normalize() -> None:
    """
    Run all tool parsers and write a unified normalized.json.

    Contract:
    - Always attempt to run every parser.
    - Never stop on a single tool failure.
    - A parser exception is treated as a runtime_error for that tool.
    """
    results: List[Dict] = []

    # ----------------------------------------------------------
    # Parse each tool artifact
    # ----------------------------------------------------------
    # If a parser fails, we still produce a standardized record
    # so the policy layer can correctly block the merge.
    # ----------------------------------------------------------
    for tool_name, parser in TOOL_PARSERS:
        try:
            # Each parser receives the raw artifact directory and returns
            # a standardized result dictionary (schemas.base_tool_result()).
            result = parser(RAW_DIR)
            results.append(result)
        except Exception as e:
            # Parser crash should never crash the whole normalization run.
            # Convert the exception into a runtime_error record.
            error_result = base_tool_result(tool_name)
            error_result["executed"] = False
            error_result["runtime_error"] = True
            error_result["metadata"] = error_result.get("metadata", {})
            error_result["metadata"]["error"] = str(e)
            results.append(error_result)

    # ----------------------------------------------------------
    # Write normalized results
    # ----------------------------------------------------------
    # This is the single input to the policy engine.
    # ----------------------------------------------------------
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    print(f"Normalized results written to {OUTPUT_FILE}")


# Allow local execution:
#   Recommended:
#     python -m quality.ci.gate.normalize
#   Still works from repo root:
#     python quality/ci/gate/normalize.py
if __name__ == "__main__":
    normalize()
