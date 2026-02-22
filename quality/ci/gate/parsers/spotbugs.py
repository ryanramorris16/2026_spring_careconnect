# ==========================================================
# spotbugs.py
# ----------------------------------------------------------
# Parses SpotBugs XML report.
#
# Expected raw artifact:
# quality/analysis/raw/spotbugs.xml
#
# Maps SpotBugs bug severity to normalized severities.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from schemas import base_tool_result


def parse_spotbugs(raw_dir: Path):
    tool_name = "spotbugs"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "spotbugs.xml"

    if not artifact.exists():
        result["runtime_error"] = True
        return result

    result["executed"] = True

    try:
        tree = ET.parse(artifact)
        root = tree.getroot()

        for bug in root.iter("BugInstance"):
            severity = bug.attrib.get("priority", "").upper()

            # SpotBugs priority:
            # 1 = High
            # 2 = Medium
            # 3 = Low

            if severity == "1":
                normalized = "high"
            elif severity == "2":
                normalized = "medium"
            elif severity == "3":
                normalized = "low"
            else:
                normalized = "info"

            result["severity_counts"][normalized] += 1
            result["violation_count"] += 1

        # Determine max severity
        if result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result