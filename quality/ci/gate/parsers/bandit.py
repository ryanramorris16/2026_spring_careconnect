# File: quality/ci/gate/parsers/bandit.py
# ==========================================================
# bandit.py
# ----------------------------------------------------------
# Bandit Parser (Python Security SAST)
#
# Purpose:
#   Parse Bandit JSON output and normalize findings into the
#   standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/bandit.json
#
# Native Bandit Severities:
#   HIGH    → Serious security issue.
#   MEDIUM  → Potential security issue.
#   LOW     → Minor issue; informational.
#
# Native Bandit Confidence levels (not used for severity mapping
# but preserved in finding metadata for human review):
#   HIGH / MEDIUM / LOW
#
# Severity Mapping (Bandit → Normalized):
#   HIGH   → high
#   MEDIUM → medium
#   LOW    → low
#   <unknown> → info
#
# Bandit JSON Structure:
#   {
#     "results": [
#       {
#         "test_id":         "B106",
#         "test_name":       "hardcoded_password_funcarg",
#         "issue_severity":  "LOW",
#         "issue_confidence":"MEDIUM",
#         "issue_text":      "Possible hardcoded password...",
#         "filename":        "quality/ci/gate/normalize.py",
#         "line_number":     42,
#         "more_info":       "https://bandit.readthedocs.io/..."
#       }
#     ]
#   }
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity

SEVERITY_MAP = {
    "high":   "high",
    "medium": "medium",
    "low":    "low",
}


def parse_bandit(raw_dir: Path) -> dict:
    """
    Parse bandit.json and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed JSON   → runtime_error=True, error in metadata.
        - Empty results [] → valid result with zero violations.
    """
    result   = base_tool_result("bandit")
    artifact = raw_dir / "bandit.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"]    = True
        return result

    result["artifact_present"] = True
    result["executed"]         = True

    try:
        with open(artifact) as f:
            data = json.load(f)

        raw_findings = data.get("results", [])
        findings     = []

        for raw in raw_findings:
            native_sev = (raw.get("issue_severity") or "low").lower()
            norm_sev   = SEVERITY_MAP.get(native_sev, "info")
            result["severity_counts"][norm_sev] += 1

            findings.append({
                "file":            raw.get("filename", "unknown"),
                "line":            raw.get("line_number", 0),
                "column":          0,
                "severity":        norm_sev,
                "native_severity": native_sev.upper(),
                "rule":            raw.get("test_id", "unknown"),
                "message":         raw.get("issue_text", ""),
                "confidence":      raw.get("issue_confidence", ""),
                "rule_url":        raw.get("more_info", ""),
            })

        result["findings"]        = findings
        result["violation_count"] = len(findings)
        result["max_severity"]    = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        result["runtime_error"]     = True
        result["metadata"]["error"] = f"JSON parse error: {e}"
    except Exception as e:
        result["runtime_error"]     = True
        result["metadata"]["error"] = f"Unexpected error: {e}"

    return result
