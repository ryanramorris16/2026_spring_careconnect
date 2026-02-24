# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/pmd.py
# ==========================================================
# pmd.py
# ----------------------------------------------------------
# PMD Parser (Java Source Static Analysis)
#
# Purpose:
#   Parse PMD XML output and normalize findings into the standard
#   schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/pmd.xml
#
# PMD severity model (priority):
#   PMD uses numeric priorities (1..5) where lower numbers are more severe.
#
# Normalization mapping used here:
#   priority 1-2 → high
#   priority 3   → medium
#   priority 4-5 → low
#
# Policy linkage:
#   - policy.yaml currently enforces:
#       severity: "medium_and_above"
#     meaning medium/high findings block the merge (if blocking=true).
#
# Notes:
#   - This parser counts every <violation> element as a finding.
#   - If later you want to capture rule names, file paths, or line numbers,
#     store a limited sample list in result["metadata"] for reporting.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result


def parse_pmd(raw_dir: Path):
    """
    Parse PMD XML and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - Parsing exceptions = runtime_error with diagnostic metadata.
    """
    tool_name = "pmd"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "pmd.xml"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing report indicates a tool execution or workflow wiring issue.
    # Treated as runtime_error (governance violation).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists and parsing will be attempted)
    result["artifact_present"] = True
    result["executed"] = True

    try:
        # Parse PMD XML
        tree = ET.parse(artifact)
        root = tree.getroot()
        # Strip namespace so tag searches work regardless of PMD version
        for elem in root.iter():
            if "}" in elem.tag:
                elem.tag = elem.tag.split("}", 1)[1]

        # ------------------------------------------------------
        # PMD XML structure includes <violation> nodes.
        # We iterate all violations and normalize their priority.
        # ------------------------------------------------------
        for violation in root.iter("violation"):
            priority = violation.attrib.get("priority", "5")

            # --------------------------------------------------
            # Normalize severity from PMD priority
            # --------------------------------------------------
            # PMD priority meaning:
            #   1 = highest priority (most severe)
            #   5 = lowest priority (least severe)
            #
            # We normalize to:
            #   1-2 -> high
            #   3   -> medium
            #   4-5 -> low
            # --------------------------------------------------
            if priority in ["1", "2"]:
                severity = "high"
            elif priority == "3":
                severity = "medium"
            else:
                severity = "low"

            # Update counters
            result["severity_counts"][severity] += 1
            result["violation_count"] += 1

        # ------------------------------------------------------
        # Determine max severity encountered
        # ------------------------------------------------------
        # This makes severity-based policy evaluation deterministic.
        # ------------------------------------------------------
        if result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"
        else:
            # No findings
            result["max_severity"] = None

    except Exception as e:
        # Any exception parsing PMD output is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result