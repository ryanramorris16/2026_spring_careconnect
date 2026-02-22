# ==========================================================
# checkstyle.py
# ----------------------------------------------------------
# Parses Checkstyle XML report.
#
# Expected raw artifact:
# quality/analysis/raw/checkstyle.xml
#
# Counts all <error> entries as violations.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from schemas import base_tool_result


def parse_checkstyle(raw_dir: Path):
    tool_name = "checkstyle"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "checkstyle.xml"

    if not artifact.exists():
        result["runtime_error"] = True
        return result

    result["executed"] = True

    try:
        tree = ET.parse(artifact)
        root = tree.getroot()

        violations = 0

        for file in root.findall("file"):
            for error in file.findall("error"):
                violations += 1

        result["violation_count"] = violations

        if violations > 0:
            result["severity_counts"]["low"] = violations
            result["max_severity"] = "low"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result