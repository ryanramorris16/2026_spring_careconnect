# File: quality/ci/gate/parsers/semgrep.py
# ==========================================================
# semgrep.py
# ----------------------------------------------------------
# Semgrep Parser (Multi-language SAST)
#
# Purpose:
#   Parse Semgrep JSON output and normalize findings into the
#   standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/semgrep.json
#
# Native Semgrep Severities:
#   ERROR     → Rule matched a high-confidence security issue.
#   WARNING   → Rule matched a potential issue; lower confidence.
#   INFO      → Informational match; lowest enforcement level.
#   INVENTORY → Inventory/audit finding; not a direct vulnerability.
#
# Severity Mapping (Semgrep → Normalized):
#   ERROR     → high
#   WARNING   → medium
#   INFO      → low
#   INVENTORY → info
#   <unknown> → info
#
# Note:
#   Semgrep does not emit a native "critical" severity. High is the ceiling.
#
# Behavior:
#   - Reads the "results" array from the Semgrep JSON artifact.
#   - Maps native severity to normalized severity per the table above.
#   - Populates findings[] with per-finding detail including CWE and OWASP.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# Semgrep JSON Structure:
#   {
#     "results": [
#       {
#         "check_id": "python.flask.security.injection.tainted-sql-string",
#         "path":     "src/main/app.py",
#         "start":    { "line": 42, "col": 5 },
#         "end":      { "line": 42, "col": 30 },
#         "extra": {
#           "severity": "ERROR",
#           "message":  "Possible SQL injection...",
#           "metadata": {
#             "cwe":   ["CWE-89: Improper Neutralization..."],
#             "owasp": ["A1:2017 - Injection"]
#           }
#         }
#       }
#     ]
#   }
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: Semgrep native → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "error":     "high",
    "warning":   "medium",
    "info":      "low",
    "inventory": "info",
}


def parse_semgrep(raw_dir: Path) -> dict:
    """
    Parse Semgrep JSON and return a standardized result dictionary.

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
        - Empty results [] → valid result with zero violations.
    """
    tool_name = "semgrep"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "semgrep.json"

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
        # Load and parse the JSON artifact
        with open(artifact) as f:
            data = json.load(f)

        # Semgrep stores all findings in the top-level "results" array.
        # An empty array is a valid result (no findings).
        raw_findings = data.get("results", [])

        # Accumulate findings as we walk the results list
        findings = []

        for raw in raw_findings:
            # Pull the "extra" block which contains severity, message, metadata
            extra    = raw.get("extra", {})
            metadata = extra.get("metadata", {})

            # Extract native severity; default to "INFO" if absent
            native_severity = extra.get("severity", "INFO").lower()

            # Map native severity to normalized severity.
            # Default to "info" for any unrecognized value.
            normalized_severity = SEVERITY_MAP.get(native_severity, "info")

            # Increment the appropriate severity bucket directly on the
            # result dict — base_tool_result already owns the structure.
            result["severity_counts"][normalized_severity] += 1

            # Extract location — Semgrep uses "start" block for line/col
            start = raw.get("start", {})

            # CWE and OWASP are lists in Semgrep metadata; default to empty
            cwe   = metadata.get("cwe", [])
            owasp = metadata.get("owasp", [])

            # Normalize to lists in case a single string was provided
            if isinstance(cwe, str):
                cwe = [cwe]
            if isinstance(owasp, str):
                owasp = [owasp]

            # Build the standardized finding record
            finding = {
                "file":            raw.get("path", "unknown"),
                "line":            start.get("line", 0),
                "column":          start.get("col", 0),
                "severity":        normalized_severity,
                "native_severity": native_severity.upper(),
                "rule":            raw.get("check_id", "unknown"),
                "message":         extra.get("message", ""),
                "cwe":             cwe,
                "owasp":           owasp,
            }
            findings.append(finding)

        # Store all individual findings
        result["findings"] = findings

        # Total number of findings
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
