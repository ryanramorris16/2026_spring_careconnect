# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/sonarqube.py
# ==========================================================
# sonarqube.py
# ----------------------------------------------------------
# SonarQube Quality Gate Parser
#
# Purpose:
#   Parse the SonarQube "Quality Gate" status output and normalize it
#   into the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/sonarqube.json
#
# Expected JSON structure (minimal contract):
#   {
#     "projectStatus": {
#       "status": "OK" | "ERROR"
#     }
#   }
#
# Behavior:
#   - If status == "OK": no violations.
#   - If status == "ERROR": represent as a single violation_count=1.
#       Why? Sonar gate is pass/fail, not a list of findings, and we want
#       the policy layer to be tool-agnostic. Converting failure into
#       violation_count > 0 makes enforcement consistent.
#   - If status is unknown/missing: treat as runtime_error (schema mismatch).
#
# Policy linkage:
#   - policy.yaml currently sets:
#       blocking: false
#     making Sonar advisory until access/configuration is ready.
#   - Once configured, change blocking to true to enforce gate strictly.
#
# Notes:
#   - SonarQube can also provide detailed conditions; this parser currently
#     only captures overall gate status. If you later want condition-level
#     detail in the report, store a limited summary in metadata.
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_sonarqube(raw_dir: Path):
    """
    Parse sonarqube.json and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error (signals tool not run / wiring issue).
      - Parsing exceptions = runtime_error with diagnostic metadata.
    """
    tool_name = "sonarqube"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "sonarqube.json"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Even though Sonar may be "advisory" in policy.yaml, missing artifacts
    # indicate the tool did not run or workflow is miswired.
    #
    # Note:
    #   Whether this blocks the merge is controlled by policy.yaml (blocking flag).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists; parsing will be attempted)
    result["executed"] = True

    try:
        # Load JSON report
        with open(artifact) as f:
            data = json.load(f)

        # Extract quality gate status in a resilient way
        status = (
            data.get("projectStatus", {})
            .get("status", "")
            .upper()
        )

        # ------------------------------------------------------
        # Normalize Sonar gate status into unified schema
        # ------------------------------------------------------
        if status == "ERROR":
            # Gate failed: represent as a single high-severity violation
            result["violation_count"] = 1
            result["severity_counts"]["high"] = 1
            result["max_severity"] = "high"

        elif status == "OK":
            # Gate passed: no violations
            result["violation_count"] = 0
            result["max_severity"] = None

        else:
            # Unknown response shape or unexpected status value
            result["runtime_error"] = True
            result["metadata"]["error"] = f"Unknown Sonar status: {status}"

    except Exception as e:
        # Any exception parsing Sonar output is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result