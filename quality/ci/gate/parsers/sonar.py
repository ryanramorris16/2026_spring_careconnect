# File: quality/ci/gate/parsers/sonar.py
# ==========================================================
# sonar.py
# ----------------------------------------------------------
# SonarQube / SonarCloud Quality Gate Parser
#
# Purpose:
#   Parse the Sonar Quality Gate JSON artifact and normalize
#   findings into the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/sonar.json
#
# Native Sonar Severities:
#   BLOCKER  → Must-fix; application behavior is critically affected.
#   CRITICAL → High-impact issue; likely security or reliability concern.
#   MAJOR    → Significant issue; may impact productivity or reliability.
#   MINOR    → Small issue; does not significantly affect the application.
#   INFO     → Informational only; lowest enforcement level.
#
# Severity Mapping (Sonar → Normalized):
#   BLOCKER  → critical
#   CRITICAL → high
#   MAJOR    → medium
#   MINOR    → low
#   INFO     → info
#   <unknown> → info
#
# Current Status: DISABLED (placeholder mode)
# ----------------------------------------------------------
# This tool is currently inactive. The CI workflow writes a
# placeholder artifact to satisfy the gate engine:
#   {"projectStatus": {"status": "OK"}, "issues": []}
#
# The parser detects this placeholder and marks the tool as
# disabled — it will not count as a runtime error or block
# the merge regardless of policy.yaml configuration.
#
# To activate SonarQube/SonarCloud:
#
#   Step 1 — Create a SonarCloud account:
#     https://sonarcloud.io
#
#   Step 2 — Add the following GitHub Actions secrets:
#     SONAR_TOKEN       → Your SonarCloud authentication token
#     SONAR_PROJECT_KEY → Your project key from SonarCloud dashboard
#     SONAR_ORG         → Your SonarCloud organization name
#
#   Step 3 — Replace the placeholder CI step in build-and-analyze.yml
#     with a real Sonar scan step using:
#     uses: SonarSource/sonarcloud-github-action@master
#
#   Step 4 — Update the CI step to write the real Sonar API response:
#     curl -u "${SONAR_TOKEN}:" \
#       "https://sonarcloud.io/api/qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}" \
#       > quality/analysis/raw/sonar.json
#
#   Step 5 — Set SONAR_ENABLED = True in this file (see below).
#
#   Step 6 — Update policy.yaml to set tools.sonar.blocking = true.
#
# Behavior (when active):
#   - Reads projectStatus.status for overall gate result.
#   - Reads issues[] for per-finding detail.
#   - Maps native Sonar severities per the table above.
#   - Populates findings[] with per-issue detail.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# Sonar JSON Structure (when active):
#   {
#     "projectStatus": {
#       "status": "ERROR"
#     },
#     "issues": [
#       {
#         "severity": "CRITICAL",
#         "message":  "Possible null pointer dereference.",
#         "component": "com.careconnect.auth:LoginService.java",
#         "line": 42,
#         "rule": "java:S2259"
#       }
#     ]
#   }
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Tool activation flag
# ----------------------------------------------------------
# Set to True once SonarQube/SonarCloud is configured.
# See activation steps in the header comments above.
# ----------------------------------------------------------
SONAR_ENABLED = False

# ----------------------------------------------------------
# Severity mapping: Sonar native → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "blocker":  "critical",
    "critical": "high",
    "major":    "medium",
    "minor":    "low",
    "info":     "info",
}


def parse_sonar(raw_dir: Path) -> dict:
    """
    Parse sonar.json and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema. When the tool
        is disabled, returns a clean result with zero violations and
        a metadata note indicating the tool is inactive.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - When SONAR_ENABLED=False, returns disabled result immediately.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed JSON   → runtime_error=True, error captured in metadata.
    """
    tool_name = "sonar"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # ----------------------------------------------------------
    # Disabled state
    # ----------------------------------------------------------
    # When SONAR_ENABLED is False, the tool is not configured.
    # Return a clean result marked as disabled so the gate engine
    # and policy layer can identify and skip it without error.
    # ----------------------------------------------------------
    if not SONAR_ENABLED:
        result["artifact_present"] = True
        result["executed"]         = False
        result["runtime_error"]    = False
        result["metadata"]["status"]  = "disabled"
        result["metadata"]["reason"]  = (
            "SonarQube/SonarCloud is not configured. "
            "See activation steps in sonar.py header comments."
        )
        return result

    # ----------------------------------------------------------
    # Active path — only reached when SONAR_ENABLED = True
    # ----------------------------------------------------------

    # Build the expected artifact path
    artifact = raw_dir / "sonar.json"

    # ----------------------------------------------------------
    # Artifact presence check
    # ----------------------------------------------------------
    # If the file does not exist, mark as runtime error and return
    # early. The policy engine will decide whether a missing artifact
    # constitutes a blocking violation.
    # ----------------------------------------------------------
    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"]    = True
        result["metadata"]["error"] = "Missing artifact: sonar.json"
        return result

    # Artifact is present; mark accordingly
    result["artifact_present"] = True
    result["executed"]         = True

    try:
        # Load and parse the JSON artifact
        with open(artifact) as f:
            data = json.load(f)

        # Extract overall gate status for metadata
        project_status = data.get("projectStatus", {})
        gate_status    = project_status.get("status", "UNKNOWN").strip().upper()

        # Store raw gate status for audit trail
        result["metadata"]["gate_status"] = gate_status

        # Walk the issues array for per-finding detail
        issues   = data.get("issues", [])
        findings = []

        for issue in issues:
            # Extract native severity; default to "INFO" if absent
            native_severity = issue.get("severity", "INFO").lower()

            # Map native severity to normalized severity.
            # Default to "info" for any unrecognized value.
            normalized_severity = SEVERITY_MAP.get(native_severity, "info")

            # Increment the appropriate severity bucket directly on the
            # result dict — base_tool_result already owns the structure.
            result["severity_counts"][normalized_severity] += 1

            # Build the standardized finding record
            finding = {
                "file":            issue.get("component", "unknown"),
                "line":            issue.get("line", 0),
                "severity":        normalized_severity,
                "native_severity": native_severity.upper(),
                "rule":            issue.get("rule", "unknown"),
                "message":         issue.get("message", ""),
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