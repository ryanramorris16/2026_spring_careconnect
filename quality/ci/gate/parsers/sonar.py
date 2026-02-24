# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/sonar.py
# ==========================================================
# sonar.py
# ----------------------------------------------------------
# Sonar (SonarQube / SonarCloud) Quality Gate Parser
#
# Purpose:
#   Parse the Sonar Quality Gate status and normalize it into the
#   standard schema defined in schemas.py.
#
# Raw Input Artifact (produced by workflow):
#   quality/analysis/raw/sonar.json
#
# Expected JSON structure (minimal contract):
#   {
#     "projectStatus": {
#       "status": "OK" | "ERROR"
#     }
#   }
#
# Normalization Strategy:
#   Sonar’s Quality Gate is effectively pass/fail (not a list of per-file
#   findings in this artifact), so we normalize as:
#
#     - OK    → violation_count = 0
#     - ERROR → violation_count = 1, max_severity = "high"
#
#   This makes policy enforcement tool-agnostic:
#     policy_engine.py only has to check "violation_count > 0" (quality_gate)
#     rather than implement Sonar-specific logic.
#
# Governance Notes:
#   - Missing artifact or malformed JSON is treated as runtime_error.
#   - Whether a runtime_error blocks the merge depends on policy.yaml
#     (tools.sonar.blocking).
#
# Future Expansion:
#   If you later want condition-level details (coverage %, duplications, etc),
#   store a summarized list in result["metadata"] without changing the
#   policy layer.
# ==========================================================

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict

# NOTE:
# We use a relative import so this works when gate.py is executed as a module:
#   python -m quality.ci.gate.gate
from ..schemas import base_tool_result


def parse_sonar(raw_dir: Path) -> Dict[str, Any]:
    """
    Parse quality/analysis/raw/sonar.json and return a standardized result.

    Parser Contract:
      - Always return the base_tool_result structure.
      - Missing artifact => runtime_error (wiring/tool execution issue).
      - Any parse/schema issue => runtime_error with diagnostic metadata.

    Args:
      raw_dir: Path to quality/analysis/raw/

    Returns:
      Standardized tool result dict for tool = "sonar"
    """
    tool_name = "sonar"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "sonar.json"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # If the artifact is missing, the tool likely did not run or was skipped.
    # We flag runtime_error so governance can detect non-deterministic runs.
    # Whether this blocks depends on policy.yaml (blocking flag).
    # ------------------------------------------------------
    if not artifact.exists():
        result["artifact_present"] = True
        result["executed"] = True
        result["runtime_error"] = True
        result["metadata"]["error"] = "Missing artifact: sonar.json"
        return result

    # Artifact exists, so the tool "executed" from the perspective of the gate.
    result["executed"] = True

    try:
        data = json.loads(artifact.read_text(encoding="utf-8"))

        status = (
            (data.get("projectStatus") or {})
            .get("status", "")
        )
        status_norm = str(status).strip().upper()

        # Store raw status for debugging/audit (policy layer should not rely on it)
        result["metadata"]["status"] = status_norm

        # ------------------------------------------------------
        # Normalize Sonar gate status
        # ------------------------------------------------------
        if status_norm == "OK":
            # Gate passed
            result["violation_count"] = 0
            result["max_severity"] = None

        elif status_norm == "ERROR":
            # Gate failed (represent as a single high severity violation)
            result["violation_count"] = 1
            result["severity_counts"]["high"] = 1
            result["max_severity"] = "high"

        else:
            # Unexpected schema or unknown status value
            result["runtime_error"] = True
            result["metadata"]["error"] = f"Unknown Sonar status value: {status_norm or '<empty>'}"

    except Exception as e:
        # Any parse failure is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to parse sonar.json: {e}"

    return result
