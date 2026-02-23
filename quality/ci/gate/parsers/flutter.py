# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/flutter.py
# ==========================================================
# flutter.py
# ----------------------------------------------------------
# Flutter Analyze Parser (Dart Analyzer)
#
# Purpose:
#   Parse the Flutter/Dart analyzer summary artifact and normalize
#   the results into the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/flutter_analyze.json
#
# Expected JSON structure (minimal contract):
#   {
#     "error_count": int,
#     "warning_count": int
#   }
#
# Behavior:
#   - Maps analyzer errors to "high" severity (blocking by policy).
#   - Maps warnings to "low" severity (informational unless policy changes).
#   - violation_count is the sum of errors + warnings.
#
# Policy linkage:
#   - policy.yaml currently enforces:
#       error_count: ">0"
#     meaning ANY analyzer error blocks the merge (if blocking=true).
#
# Notes:
#   - This parser assumes the workflow produces a simplified JSON summary
#     rather than parsing raw text output.
#   - If later you want rule-level detail (file/line/message), extend the
#     raw artifact schema and store samples in result["metadata"].
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_flutter(raw_dir: Path):
    """
    Parse flutter_analyze.json and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - This function should not raise exceptions outward.
    """
    tool_name = "flutter_analyze"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "flutter_analyze.json"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing artifact means:
    #   - tool did not run
    #   - tool failed before writing output
    #   - workflow path mismatch
    # Treated as runtime_error (governance violation).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists)
    result["executed"] = True

    try:
        # Load JSON summary produced by the workflow
        with open(artifact) as f:
            data = json.load(f)

        # Extract counts with safe defaults
        error_count = data.get("error_count", 0)
        warning_count = data.get("warning_count", 0)

        # Total findings count (used for reporting / generic rules)
        result["violation_count"] = error_count + warning_count

        # ------------------------------------------------------
        # Severity mapping
        # ------------------------------------------------------
        # We treat analyzer errors as "high" severity because they block builds.
        # We treat warnings as "low" by default (configurable via policy.yaml).
        # ------------------------------------------------------
        result["severity_counts"]["high"] = error_count
        result["severity_counts"]["low"] = warning_count

        # Determine max severity encountered
        if error_count > 0:
            result["max_severity"] = "high"
        elif warning_count > 0:
            result["max_severity"] = "low"
        else:
            result["max_severity"] = None

    except Exception as e:
        # Any parsing exception is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result