# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/semgrep.py
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
# Semgrep JSON structure (common):
#   {
#     "results": [
#       {
#         "extra": { "severity": "HIGH" | "MEDIUM" | ... },
#         ...
#       }
#     ]
#   }
#
# Behavior:
#   - Counts each entry in "results" as one finding.
#   - Normalizes Semgrep severity into:
#       critical | high | medium | low | info
#   - Does NOT apply enforcement thresholds (policy.yaml controls that).
#
# Policy linkage:
#   - policy.yaml currently enforces:
#       severity: "high_and_above"
#     meaning HIGH/CRITICAL findings block the merge (if blocking=true).
#
# Notes:
#   - If you later want to include rule IDs, file paths, or line numbers
#     in the human report, store a limited sample list in metadata.
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result


def parse_semgrep(raw_dir: Path):
    """
    Parse Semgrep JSON and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - Parsing exceptions = runtime_error with diagnostic metadata.
    """
    tool_name = "semgrep"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "semgrep.json"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing report indicates:
    #   - tool did not run
    #   - workflow path mismatch
    #   - tool failed before writing output
    # Treated as runtime_error (governance violation).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists; parsing will be attempted)
    result["artifact_present"] = True
    result["executed"] = True

    try:
        # Load JSON report
        with open(artifact) as f:
            data = json.load(f)

        # Semgrep stores findings in the "results" array.
        findings = data.get("results", [])

        # ------------------------------------------------------
        # Extract + normalize each finding severity
        # ------------------------------------------------------
        for finding in findings:
            # Semgrep severity commonly appears under extra.severity
            severity = finding.get("extra", {}).get("severity", "").lower()

            # Normalize Semgrep-provided severity into shared vocabulary.
            if severity in ("critical",):
                normalized = "critical"
            elif severity in ("high", "error"):
                normalized = "high"
            elif severity == "medium":
                normalized = "medium"
            elif severity == "low":
                normalized = "low"
            else:
                # Unknown/missing severity is downgraded to info, but still counted.
                normalized = "info"

            # Update counters
            result["severity_counts"][normalized] += 1
            result["violation_count"] += 1

        # ------------------------------------------------------
        # Determine max severity encountered
        # ------------------------------------------------------
        if result["severity_counts"]["critical"] > 0:
            result["max_severity"] = "critical"
        elif result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"
        else:
            # No findings
            result["max_severity"] = None

    except Exception as e:
        # Any exception parsing Semgrep output is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result