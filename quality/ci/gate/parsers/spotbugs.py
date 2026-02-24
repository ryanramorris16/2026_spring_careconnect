# File: quality/ci/gate/parsers/spotbugs.py
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
# Native SpotBugs Severities (Priority Model):
#   SpotBugs reports a numeric "priority" on each BugInstance.
#
#   Priority 1 → High    (most severe)
#   Priority 2 → Medium
#   Priority 3 → Low     (least severe)
#
# Severity Mapping (SpotBugs Priority → Normalized):
#   Priority 1   → high
#   Priority 2   → medium
#   Priority 3   → low
#   <unknown>    → info
#
# Behavior:
#   - Parses every <BugInstance> node in the XML report.
#   - Maps native priority to normalized severity per the table above.
#   - Populates findings[] with per-bug detail.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# SpotBugs XML Structure:
#   <BugCollection>
#     <BugInstance type="NP_NULL_ON_SOME_PATH" priority="1" rank="4"
#                  abbrev="NP" category="CORRECTNESS">
#       <Class classname="com.careconnect.auth.LoginService" />
#       <Method name="authenticate" signature="..." isStatic="false" />
#       <SourceLine classname="com.careconnect.auth.LoginService"
#                   start="42" end="42"
#                   sourcefile="LoginService.java"
#                   sourcepath="com/careconnect/auth/LoginService.java" />
#       <ShortMessage>Null pointer dereference...</ShortMessage>
#     </BugInstance>
#   </BugCollection>
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: SpotBugs native priority → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "1": "high",
    "2": "medium",
    "3": "low",
}


def parse_spotbugs(raw_dir: Path) -> dict:
    """
    Parse spotbugs.xml and return a standardized result dictionary.

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
    tool_name = "spotbugs"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "spotbugs.xml"

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

        # Accumulate findings as we walk the XML tree
        findings = []

        # ----------------------------------------------------------
        # Walk all BugInstance nodes
        # ----------------------------------------------------------
        # SpotBugs writes one <BugInstance> per detected bug.
        # Key attributes: type (rule), priority, category.
        # Child elements provide class, method, and source location.
        # ----------------------------------------------------------
        for bug in root.iter("BugInstance"):

            # Extract native priority; default to unknown if absent
            native_priority = bug.attrib.get("priority", "")

            # Map native priority to normalized severity.
            # Default to "info" for any unrecognized or missing priority.
            normalized_severity = SEVERITY_MAP.get(native_priority, "info")

            # Increment the appropriate severity bucket directly on the
            # result dict — base_tool_result already owns the structure.
            result["severity_counts"][normalized_severity] += 1

            # --------------------------------------------------
            # Extract source location from <SourceLine> child.
            # SpotBugs may include multiple SourceLine elements;
            # we use the first one associated with the BugInstance.
            # --------------------------------------------------
            source_line = bug.find("SourceLine")
            if source_line is not None:
                source_file = source_line.attrib.get("sourcepath", "unknown")
                line_start  = int(source_line.attrib.get("start", 0))
            else:
                source_file = "unknown"
                line_start  = 0

            # Extract the human-readable short message if present
            short_msg = bug.find("ShortMessage")
            message   = short_msg.text.strip() if short_msg is not None and short_msg.text else ""

            # Extract class name for additional context
            class_node = bug.find("Class")
            class_name = class_node.attrib.get("classname", "unknown") if class_node is not None else "unknown"

            # Build the standardized finding record
            finding = {
                "file":            source_file,
                "line":            line_start,
                "severity":        normalized_severity,
                "native_severity": f"priority {native_priority}" if native_priority else "unknown",
                "rule":            bug.attrib.get("type", "unknown"),
                "category":        bug.attrib.get("category", "unknown"),
                "class":           class_name,
                "message":         message,
            }
            findings.append(finding)

        # Store all individual findings
        result["findings"] = findings

        # Total number of bugs found
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
