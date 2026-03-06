"""
HTMLHint Parser (HTML Static Analysis)

Purpose
-------
Parse HTMLHint JSON output and normalize findings into the
standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/htmlhint.json

Native HTMLHint Severities
--------------------------
error
    Rule violation that should be fixed.
warning
    Potential issue; advisory.

Severity Mapping
----------------
HTMLHint -> Normalized

- error -> high
- warning -> medium
- unknown -> low

HTMLHint JSON Structure
-----------------------
[
  {
    "filePath": "frontend/web/index.html",
    "messages": [
      {
        "rule": { "id": "doctype-first", "description": "..." },
        "message": "Doctype must be declared first.",
        "line": 1,
        "col": 1,
        "type": "error"
      }
    ]
  }
]
"""

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


SEVERITY_MAP = {
    "error": "high",
    "warning": "medium",
}


def parse_htmlhint(raw_dir: Path) -> dict:
    """
    Parse htmlhint.json and return a standardized result dictionary.

    Parameters
    ----------
    raw_dir : Path
        Directory containing raw tool output artifacts.

    Returns
    -------
    dict
        Result dictionary conforming to the base_tool_result schema.

    Contract
    --------
    - Always returns a base_tool_result structure.
    - Never raises exceptions outward.
    - Missing artifact sets artifact_present=False and runtime_error=True.
    - Malformed JSON sets runtime_error=True and records the error in metadata.
    - Empty array is treated as a valid execution with zero violations.
    """
    result = base_tool_result("htmlhint")
    artifact = raw_dir / "htmlhint.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        return result

    result["artifact_present"] = True
    result["executed"] = True

    try:
        with open(artifact, "r", encoding="utf-8") as f:
            data = json.load(f)

        file_results = data if isinstance(data, list) else []
        findings = []

        for file_result in file_results:
            file_path = file_result.get("file", file_result.get("filePath", "unknown"))
            messages = file_result.get("messages", [])

            for msg in messages:
                native_sev = (msg.get("type") or "warning").lower()
                norm_sev = SEVERITY_MAP.get(native_sev, "low")
                result["severity_counts"][norm_sev] += 1

                rule = msg.get("rule", {})
                rule_id = (
                    rule.get("id", "unknown") if isinstance(rule, dict) else str(rule)
                )

                findings.append(
                    {
                        "file": file_path,
                        "line": msg.get("line", 0),
                        "column": msg.get("col", 0),
                        "severity": norm_sev,
                        "native_severity": native_sev,
                        "rule": rule_id,
                        "message": msg.get("message", ""),
                    }
                )

        result["findings"] = findings
        result["violation_count"] = len(findings)
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"JSON parse error: {e}"
    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Unexpected error: {e}"

    return result
