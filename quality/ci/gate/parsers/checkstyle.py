# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/checkstyle.py
# ==========================================================
# checkstyle.py
# ----------------------------------------------------------
# Checkstyle Parser (Java Style Enforcement)
#
# Purpose:
#   Parse the Checkstyle XML report and convert it into the
#   standardized schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/checkstyle.xml
#
# Behavior:
#   - Counts every <error> node as a violation.
#   - Does NOT apply policy thresholds (that belongs in policy_engine.py).
#   - Maps all Checkstyle findings to "low" severity by default.
#
# Notes:
#   - Checkstyle primarily enforces style and formatting rules.
#   - If you later want severity-aware mapping (error/warning/info),
#     this is the place to implement that normalization.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result


def parse_checkstyle(raw_dir: Path):
    """
    Parse Checkstyle XML and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Never raise exceptions outward.
      - Missing artifact = runtime_error.
    """
    tool_name = "checkstyle"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Construct expected artifact path
    artifact = raw_dir / "checkstyle.xml"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # If the artifact is missing, treat as runtime error.
    # Policy layer will determine whether this blocks.
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists and parser will attempt to read it)
    result["artifact_present"] = True
    result["executed"] = True

    try:
        # Parse XML document
        tree = ET.parse(artifact)
        root = tree.getroot()

        violations = 0

        # ------------------------------------------------------
        # Checkstyle XML structure:
        #
        # <checkstyle>
        #   <file name="...">
        #     <error line="..." column="..." severity="..." message="..." />
        #   </file>
        # </checkstyle>
        #
        # We treat each <error> node as one violation.
        # ------------------------------------------------------
        for file_node in root.findall("file"):
            for error_node in file_node.findall("error"):
                violations += 1

        # Set total violation count
        result["violation_count"] = violations

        # ------------------------------------------------------
        # Severity Mapping
        # ------------------------------------------------------
        # Currently:
        #   All Checkstyle findings are normalized to "low".
        #
        # Rationale:
        #   Style violations are typically lower risk.
        #
        # Future enhancement:
        #   Use error_node.attrib["severity"] to map:
        #     error → high
        #     warning → medium
        #     info → low
        # ------------------------------------------------------
        if violations > 0:
            result["severity_counts"]["low"] = violations
            result["max_severity"] = "low"

    except Exception as e:
        # ------------------------------------------------------
        # Any parsing exception is treated as a runtime_error.
        # This ensures:
        #   - Malformed XML
        #   - Unexpected schema changes
        #   - Corrupted artifacts
        # are surfaced as governance violations.
        # ------------------------------------------------------
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result
