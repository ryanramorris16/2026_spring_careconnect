"""
SpotBugs Parser (Java Bytecode Static Analysis)

Purpose
-------
Parse SpotBugs XML output and normalize findings into the
standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/spotbugs.xml

Native SpotBugs Severities
--------------------------
SpotBugs reports a numeric priority on each BugInstance.

Priority 1
    High and most severe.
Priority 2
    Medium.
Priority 3
    Low and least severe.

Severity Mapping
----------------
SpotBugs -> Normalized

- Priority 1 -> high
- Priority 2 -> medium
- Priority 3 -> low
- unknown -> info

Behavior
--------
- Parses every <BugInstance> node in the XML report.
- Maps native priority to normalized severity.
- Populates findings with per-bug detail.
- Counts violations per normalized severity level.
- Sets max_severity to the highest normalized severity found.
- Does not apply policy thresholds.

SpotBugs XML Structure
----------------------
<BugCollection>
  <BugInstance type="NP_NULL_ON_SOME_PATH" priority="1" rank="4"
               abbrev="NP" category="CORRECTNESS">
    <Class classname="com.careconnect.auth.LoginService" />
    <Method name="authenticate" signature="..." isStatic="false" />
    <SourceLine classname="com.careconnect.auth.LoginService"
                start="42" end="42"
                sourcefile="LoginService.java"
                sourcepath="com/careconnect/auth/LoginService.java" />
    <ShortMessage>Null pointer dereference...</ShortMessage>
  </BugInstance>
</BugCollection>
"""

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


SEVERITY_MAP = {
    "1": "high",
    "2": "medium",
    "3": "low",
}


def parse_spotbugs(raw_dir: Path) -> dict:
    """
    Parse spotbugs.xml and return a standardized result dictionary.

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
    tool_name = "spotbugs"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "spotbugs.xml"

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

        for bug in root.iter("BugInstance"):
            native_priority = bug.attrib.get("priority", "")
            normalized_severity = SEVERITY_MAP.get(native_priority, "info")
            result["severity_counts"][normalized_severity] += 1

            source_line = bug.find("SourceLine")
            if source_line is not None:
                source_file = source_line.attrib.get("sourcepath", "unknown")
                line_start = int(source_line.attrib.get("start", 0))
            else:
                source_file = "unknown"
                line_start = 0

            short_msg = bug.find("ShortMessage")
            message = (
                short_msg.text.strip()
                if short_msg is not None and short_msg.text
                else ""
            )

            class_node = bug.find("Class")
            class_name = (
                class_node.attrib.get("classname", "unknown")
                if class_node is not None
                else "unknown"
            )

            finding = {
                "file": source_file,
                "line": line_start,
                "severity": normalized_severity,
                "native_severity": (
                    f"priority {native_priority}" if native_priority else "unknown"
                ),
                "rule": bug.attrib.get("type", "unknown"),
                "category": bug.attrib.get("category", "unknown"),
                "class": class_name,
                "message": message,
            }
            findings.append(finding)

        result["findings"] = findings
        result["violation_count"] = len(findings)
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except ET.ParseError as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"XML parse error: {e}"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Unexpected error: {e}"

    return result
