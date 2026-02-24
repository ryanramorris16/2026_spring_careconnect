 #File: quality/ci/gate/parsers/pmd.py
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
# Native PMD Severities (Priority Model):
#   PMD uses numeric priorities (1–5) where lower numbers are more severe.
#
#   Priority 1 → High Priority         (most severe)
#   Priority 2 → Medium High Priority
#   Priority 3 → Medium Priority
#   Priority 4 → Medium Low Priority
#   Priority 5 → Low Priority          (least severe)
#
# Severity Mapping (PMD Priority → Normalized):
#   Priority 1 → critical
#   Priority 2 → high
#   Priority 3 → medium
#   Priority 4 → low
#   Priority 5 → info
#   <unknown>  → info
#
# Behavior:
#   - Parses every <violation> node across all <file> nodes.
#   - Maps native priority to normalized severity per the table above.
#   - Populates findings[] with per-violation detail.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# PMD XML Structure:
#   <pmd>
#     <file name="/path/to/File.java">
#       <violation beginline="12" endline="12"
#                  begincolumn="1" endcolumn="10"
#                  rule="UnusedVariable" ruleset="Best Practices"
#                  priority="2" externalInfoUrl="https://...">
#         Description of the violation.
#       </violation>
#     </file>
#   </pmd>
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: PMD native priority → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "1": "critical",
    "2": "high",
    "3": "medium",
    "4": "low",
    "5": "info",
}


def parse_pmd(raw_dir: Path) -> dict:
    """
    Parse PMD XML and return a standardized result dictionary.

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
    tool_name = "pmd"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "pmd.xml"

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

        # ----------------------------------------------------------
        # Namespace stripping
        # ----------------------------------------------------------
        # PMD XML may include a namespace prefix on all tags, e.g.:
        #   {net.sourceforge.pmd}pmd
        # Stripping the namespace ensures tag searches work correctly
        # regardless of which PMD version produced the artifact.
        # ----------------------------------------------------------
        for elem in root.iter():
            if "}" in elem.tag:
                elem.tag = elem.tag.split("}", 1)[1]

        # Accumulate findings as we walk the XML tree
        findings = []

        # ----------------------------------------------------------
        # Walk the XML tree
        # ----------------------------------------------------------
        # Structure: <pmd> → <file name="..."> → <violation .../>
        # Each <violation> node represents one PMD finding.
        # ----------------------------------------------------------
        for file_node in root.findall("file"):
            file_path = file_node.attrib.get("name", "unknown")

            for violation in file_node.findall("violation"):

                # Extract native priority from the <violation> node
                native_priority = violation.attrib.get("priority", "5")

                # Map native priority to normalized severity.
                # Default to "info" for any unrecognized priority value.
                normalized_severity = SEVERITY_MAP.get(native_priority, "info")

                # Increment the appropriate severity bucket directly on the
                # result dict — base_tool_result already owns the structure.
                result["severity_counts"][normalized_severity] += 1

                # Violation text content is the human-readable description
                message = (violation.text or "").strip()

                # Build the standardized finding record
                finding = {
                    "file":            file_path,
                    "line":            int(violation.attrib.get("beginline", 0)),
                    "severity":        normalized_severity,
                    "native_severity": f"priority {native_priority}",
                    "rule":            violation.attrib.get("rule", "unknown"),
                    "ruleset":         violation.attrib.get("ruleset", "unknown"),
                    "message":         message,
                    "rule_url":        violation.attrib.get("externalInfoUrl", ""),
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
