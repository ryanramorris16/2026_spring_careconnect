"""
Checkstyle Parser (Java Style Enforcement)

Purpose
-------
Parse the Checkstyle XML report and convert its findings into the
standardized schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/checkstyle.xml

Native Checkstyle Severities
----------------------------
error
    A rule violation treated as an error.
warning
    A rule violation treated as a warning.
info
    Informational finding at the lowest enforcement level.
ignore
    Suppressed rule that does not appear in XML output.

Severity Mapping
----------------
Checkstyle -> Normalized

- error -> high
- warning -> medium
- info -> low

Behavior
--------
- Parses every <error> node across all <file> nodes.
- Maps native severity to normalized severity.
- Populates findings with per-violation detail.
- Counts violations per normalized severity level.
- Sets max_severity to the highest normalized severity found.
- Does not apply policy thresholds.

Checkstyle XML Structure
------------------------
<checkstyle>
  <file name="path/to/File.java">
    <error line="12" column="4" severity="error"
           message="..." source="com.puppycrawl.tools..."/>
  </file>
</checkstyle>
"""

import xml.etree.ElementTree as ET
from pathlib import Path

from quality.ci.gate.schemas import base_tool_result
from quality.ci.gate.utils import determine_max_severity


SEVERITY_MAP = {
    "error": "high",
    "warning": "medium",
    "info": "low",
}


def parse_checkstyle(raw_dir: Path) -> dict:
    """
    Parse Checkstyle XML and return a standardized result dictionary.

    Parameters
    ----------
    raw_dir : Path
        Directory containing raw tool output artifacts.

    Returns
    -------
    dict
        Result dictionary conforming to the base_tool_result schema,
        including findings, severity counts, and max_severity.

    Contract
    --------
    - Always returns a base_tool_result structure.
    - Never raises exceptions outward.
    - Missing artifact sets artifact_present=False and runtime_error=True.
    - Malformed XML sets runtime_error=True and records the error in metadata.
    """
    tool_name = "checkstyle"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "checkstyle.xml"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        return result

    result["artifact_present"] = True
    result["executed"] = True

    try:
        tree = ET.parse(artifact)
        root = tree.getroot()
        findings = []

        for file_node in root.findall("file"):
            file_path = file_node.attrib.get("name", "unknown")

            for error_node in file_node.findall("error"):
                native_severity = error_node.attrib.get("severity", "info")
                message = error_node.attrib.get("message", "")
                line = error_node.attrib.get("line", "0")
                column = error_node.attrib.get("column", "0")
                rule = error_node.attrib.get("source", "unknown")

                normalized_severity = SEVERITY_MAP.get(native_severity, "low")
                result["severity_counts"][normalized_severity] += 1

                finding = {
                    "file": file_path,
                    "line": int(line) if line.isdigit() else 0,
                    "column": int(column) if column.isdigit() else 0,
                    "severity": normalized_severity,
                    "native_severity": native_severity,
                    "message": message,
                    "rule": rule,
                }
                findings.append(finding)

        result["findings"] = findings
        result["violation_count"] = len(findings)
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except (ET.ParseError, OSError, TypeError, ValueError, KeyError) as error:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Checkstyle parse error: {error}"

    return result
