# File: quality/ci/gate/parsers/pylint.py
# ==========================================================
# pylint.py
# ----------------------------------------------------------
# Pylint Parser (Python Static Analysis)
#
# Purpose:
#   Parse Pylint JSON output and normalize findings into the
#   standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/pylint.json
#
# Native Pylint Message Types:
#   error      (E) → Code error; likely a bug.
#   warning    (W) → Potential issue.
#   convention (C) → Style violation.
#   refactor   (R) → Refactoring suggestion.
#   fatal      (F) → Pylint itself crashed on this file.
#
# Severity Mapping (Pylint → Normalized):
#   fatal      → critical
#   error      → high
#   warning    → medium
#   refactor   → low
#   convention → low
#   <unknown>  → info
#
# Pylint JSON Structure:
#   [
#     {
#       "type":    "error",
#       "module":  "quality.ci.gate.normalize",
#       "path":    "quality/ci/gate/normalize.py",
#       "line":    42,
#       "column":  4,
#       "symbol":  "undefined-variable",
#       "message": "Undefined variable 'foo'"
#     }
#   ]
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity

SEVERITY_MAP = {
    "fatal":      "critical",
    "error":      "high",
    "warning":    "medium",
    "refactor":   "low",
    "convention": "low",
}


def parse_pylint(raw_dir: Path) -> dict:
    """
    Parse pylint.json and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed JSON   → runtime_error=True, error in metadata.
        - Empty array []   → valid result with zero violations.
    """
    result   = base_tool_result("pylint")
    artifact = raw_dir / "pylint.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"]    = True
        return result

    result["artifact_present"] = True
    result["executed"]         = True

    try:
        with open(artifact) as f:
            data = json.load(f)

        # Pylint JSON is a top-level array of message objects
        raw_findings = data if isinstance(data, list) else []
        findings     = []

        for raw in raw_findings:
            native_sev  = (raw.get("type") or "warning").lower()
            norm_sev    = SEVERITY_MAP.get(native_sev, "info")
            result["severity_counts"][norm_sev] += 1

            findings.append({
                "file":            raw.get("path", "unknown"),
                "line":            raw.get("line", 0),
                "column":          raw.get("column", 0),
                "severity":        norm_sev,
                "native_severity": native_sev,
                "rule":            raw.get("symbol", "unknown"),
                "message":         raw.get("message", ""),
            })

        result["findings"]        = findings
        result["violation_count"] = len(findings)
        result["max_severity"]    = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        result["runtime_error"]       = True
        result["metadata"]["error"]   = f"JSON parse error: {e}"
    except Exception as e:
        result["runtime_error"]       = True
        result["metadata"]["error"]   = f"Unexpected error: {e}"

    return result
