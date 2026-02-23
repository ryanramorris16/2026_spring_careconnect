# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/spotbugs.py
# ==========================================================
# spotbugs.py
# ----------------------------------------------------------
# SpotBugs Parser (Java Bytecode Static Analysis)
#
# Purpose:
#   Parse SpotBugs XML output and normalize findings into the
#   standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/spotbugs.xml
#
# SpotBugs severity model (priority):
#   SpotBugs reports a numeric "priority" on each BugInstance:
#     1 = High
#     2 = Medium
#     3 = Low
#
# Normalization mapping used here:
#   priority 1 → high
#   priority 2 → medium
#   priority 3 → low
#   unknown/missing → info (still counted, but lowest severity)
#
# Policy linkage:
#   - policy.yaml currently enforces:
#       severity: "medium_and_above"
#     meaning medium/high findings block the merge (if blocking=true).
#
# Notes:
#   - This parser currently counts each <BugInstance> as one finding.
#   - If you later want rule patterns, class/method, or source file info
#     in the human report, store a limited sample list in metadata.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result


def parse_spotbugs(raw_dir: Path):
    """
    Parse spotbugs.xml and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - Parsing exceptions = runtime_error with diagnostic metadata.
    """
    tool_name = "spotbugs"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "spotbugs.xml"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing report indicates:
    #   - tool did not run
    #   - workflow output path mismatch
    #   - tool failed before writing output
    # Treated as runtime_error (governance violation).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists; parsing will be attempted)
    result["executed"] = True

    try:
        # Parse XML report
        tree = ET.parse(artifact)
        root = tree.getroot()

        # ------------------------------------------------------
        # SpotBugs XML structure includes <BugInstance> nodes.
        # Each BugInstance has a "priority" attribute (1..3).
        # ------------------------------------------------------
        for bug in root.iter("BugInstance"):
            priority = bug.attrib.get("priority", "")

            # --------------------------------------------------
            # Normalize priority into our shared severity vocabulary
            # --------------------------------------------------
            # SpotBugs priority:
            #   1 = High
            #   2 = Medium
            #   3 = Low
            #
            # Unknown or missing values are downgraded to "info"
            # but still counted as findings.
            # --------------------------------------------------
            if priority == "1":
                normalized = "high"
            elif priority == "2":
                normalized = "medium"
            elif priority == "3":
                normalized = "low"
            else:
                normalized = "info"

            # Update counters
            result["severity_counts"][normalized] += 1
            result["violation_count"] += 1

        # ------------------------------------------------------
        # Determine max severity encountered
        # ------------------------------------------------------
        if result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"
        elif result["severity_counts"]["info"] > 0:
            result["max_severity"] = "info"
        else:
            # No findings
            result["max_severity"] = None

    except Exception as e:
        # Any exception parsing SpotBugs output is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result