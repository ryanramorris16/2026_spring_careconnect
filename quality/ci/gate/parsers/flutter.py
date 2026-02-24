# File: quality/ci/gate/parsers/flutter.py
# ==========================================================
# flutter.py
# ----------------------------------------------------------
# Flutter Analyze Parser (Dart Static Analyzer)
#
# Purpose:
#   Parse the Flutter/Dart analyzer JSON artifact and normalize
#   findings into the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/flutter_analyze.json
#
# Native Flutter/Dart Analyzer Severities:
#   error   → Compile-time or analysis error; build will fail.
#   warning → Potential issue; build may succeed.
#   info    → Informational; lowest enforcement level.
#   hint    → Style or best-practice suggestion.
#
# Severity Mapping (Flutter → Normalized):
#   error   → high
#   warning → medium
#   info    → low
#   hint    → info
#   <unknown> → info
#
# Behavior:
#   - Reads the structured issues array written by the CI workflow step.
#   - Maps native severity to normalized severity per the table above.
#   - Populates findings[] with per-issue detail.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# Expected artifact structure (written by workflow step):
#   {
#     "issues": [
#       {
#         "severity": "error",
#         "message":  "The method 'login' isn't defined.",
#         "file":     "lib/auth/login.dart",
#         "line":     34,
#         "column":   1,
#         "rule":     "undefined_method"
#       }
#     ]
#   }
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: Flutter native → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "error":   "high",
    "warning": "medium",
    "info":    "low",
    "hint":    "info",
}


def parse_flutter(raw_dir: Path) -> dict:
    """
    Parse flutter_analyze.json and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema, populated
        with findings, severity counts, and max_severity.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed JSON   → runtime_error=True, error captured in metadata.
        - Empty issues []  → valid result with zero violations.
    """
    tool_name = "flutter_analyze"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "flutter_analyze.json"

    # ----------------------------------------------------------
    # Artifact presence check
    # ----------------------------------------------------------
    # If the file does not exist, mark as runtime error and return
    # early. The policy engine will decide whether a missing artifact
    # constitutes a blocking violation.
    # ----------------------------------------------------------
    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        return result

    # Artifact is present; mark accordingly
    result["artifact_present"] = True
    result["executed"] = True

    try:
        # Load and parse the structured JSON artifact
        with open(artifact) as f:
            data = json.load(f)

        # The workflow step writes a top-level "issues" array.
        # An empty array is a valid result (no findings).
        issues = data.get("issues", [])

        # Accumulate findings as we walk the issues list
        findings = []

        for issue in issues:
            # Extract native severity; default to "info" if absent
            native_severity = issue.get("severity", "info").lower()

            # Map native severity to normalized severity.
            # Default to "info" for any unrecognized value.
            normalized_severity = SEVERITY_MAP.get(native_severity, "info")

            # Increment the appropriate severity bucket directly on the
            # result dict — base_tool_result already owns the structure.
            result["severity_counts"][normalized_severity] += 1

            # Build the standardized finding record
            finding = {
                "file":            issue.get("file", "unknown"),
                "line":            issue.get("line", 0),
                "column":          issue.get("column", 0),
                "severity":        normalized_severity,
                "native_severity": native_severity,
                "message":         issue.get("message", ""),
                "rule":            issue.get("rule", "unknown"),
            }
            findings.append(finding)

        # Store all individual findings
        result["findings"] = findings

        # Total number of issues found
        result["violation_count"] = len(findings)

        # Determine max_severity using the shared utility function
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        # ----------------------------------------------------------
        # Malformed or unparseable JSON.
        # Captured separately from generic exceptions so the error
        # type is explicit in the metadata.
        # ----------------------------------------------------------
        result["runtime_error"] = True
        result["metadata"]["error"] = f"JSON parse error: {e}"

    except Exception as e:
        # ----------------------------------------------------------
        # Catch-all for unexpected failures (I/O errors, schema
        # changes, etc.) to ensure the pipeline never crashes on a
        # single parser. Surfaced as a runtime_error so the policy
        # engine can flag it as a governance concern.
        # ----------------------------------------------------------
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Unexpected error: {e}"

    return result
