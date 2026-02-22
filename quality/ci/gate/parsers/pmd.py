# ==========================================================
# pmd.py
# ----------------------------------------------------------
# Parses PMD XML report.
#
# Expected raw artifact:
# quality/analysis/raw/pmd.xml
#
# Maps PMD priority values to normalized severities.
# ==========================================================

import xml.etree.ElementTree as ET
from pathlib import Path

from schemas import base_tool_result


def parse_pmd(raw_dir: Path):
    tool_name = "pmd"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "pmd.xml"

    if not artifact.exists():
        result["runtime_error"] = True
        return result

    result["executed"] = True

    try:
        tree = ET.parse(artifact)
        root = tree.getroot()

        for violation in root.iter("violation"):
            priority = violation.attrib.get("priority", "5")

            # Normalize severity
            if priority in ["1", "2"]:
                severity = "high"
            elif priority == "3":
                severity = "medium"
            else:
                severity = "low"

            result["severity_counts"][severity] += 1
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