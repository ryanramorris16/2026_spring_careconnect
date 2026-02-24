# File: quality/ci/gate/parsers/checkstyle.py
# ==========================================================
# checkstyle.py
# ----------------------------------------------------------
# Checkstyle Parser (Java Style Enforcement)
#
# Purpose:
#   Parse the Checkstyle XML report and convert its findings
#   into the standardized schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/checkstyle.xml
#
# Native Checkstyle Severities:
#   error   → A rule violation that is treated as an error.
#   warning → A rule violation treated as a warning.
#   info    → Informational finding, lowest enforcement level.
#   ignore  → Rule is suppressed; never appears in XML output.
#
# Severity Mapping (Checkstyle → Normalized):
#   error   → high
#   warning → medium
#   info    → low
#
# Behavior:
#   - Parses every <error> node across all <file> nodes.
#   - Maps native severity to normalized severity per the table above.
#   - Populates findings[] with per-violation detail.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml owns that).
#
# Checkstyle XML Structure:
#   <checkstyle>
#     <file name="path/to/File.java">
#       <error line="12" column="4" severity="error"
#              message="..." source="com.puppycrawl.tools..."/>
#     </file>
#   </checkstyle>
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: Checkstyle native → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "error":   "high",
    "warning": "medium",
    "info":    "low",
}


def parse_checkstyle(raw_dir: Path) -> dict:
    """
    Parse Checkstyle XML and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema, populated
        with findings, severity counts, and max_severity.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed XML    → runtime_error=True, error captured in metadata.
    """
    tool_name = "checkstyle"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "checkstyle.xml"

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
        # Parse the XML document from disk
        tree = ET.parse(artifact)
        root = tree.getroot()

        # Accumulate findings as we walk the tree.
        # severity_counts is already initialized with all five levels
        # (critical, high, medium, low, info) by base_tool_result — do
        # not reinitialize it here or schema keys will be lost.
        findings = []

        # ----------------------------------------------------------
        # Walk the XML tree
        # ----------------------------------------------------------
        # Structure: <checkstyle> → <file name="..."> → <error .../>
        # Each <error> node represents one Checkstyle violation.
        # ----------------------------------------------------------
        for file_node in root.findall("file"):
            file_path = file_node.attrib.get("name", "unknown")

            for error_node in file_node.findall("error"):

                # Extract native attributes from the <error> node
                native_severity = error_node.attrib.get("severity", "info")
                message         = error_node.attrib.get("message", "")
                line            = error_node.attrib.get("line", "0")
                column          = error_node.attrib.get("column", "0")
                # 'source' contains the fully-qualified rule class name
                rule            = error_node.attrib.get("source", "unknown")

                # Map native severity to normalized severity.
                # Default to "low" for any unrecognized severity value.
                normalized_severity = SEVERITY_MAP.get(native_severity, "low")

                # Increment the appropriate severity bucket directly on
                # the result dict — base_tool_result already owns the structure.
                result["severity_counts"][normalized_severity] += 1

                # Build the standardized finding record
                finding = {
                    "file":            file_path,
                    "line":            int(line) if line.isdigit() else 0,
                    "column":          int(column) if column.isdigit() else 0,
                    "severity":        normalized_severity,
                    "native_severity": native_severity,
                    "message":         message,
                    "rule":            rule,
                }
                findings.append(finding)

        # Store all individual findings
        result["findings"] = findings

        # Total number of violations found
        result["violation_count"] = len(findings)

        # Determine max_severity using the shared utility function
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except ET.ParseError as e:
        # ----------------------------------------------------------
        # Malformed or unparseable XML.
        # Captured separately from generic exceptions so the error
        # type is explicit in the metadata.
        # ----------------------------------------------------------
        result["runtime_error"] = True
        result["metadata"]["error"] = f"XML parse error: {e}"

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
