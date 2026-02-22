# ==========================================================
# dependency_check.py
# ----------------------------------------------------------
# Parses OWASP Dependency-Check JSON report.
#
# Expected raw artifact:
# quality/analysis/raw/dependency_check.json
#
# Extracts vulnerabilities and normalizes severity.
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_dependency_check(raw_dir: Path):
    tool_name = "dependency_check"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "dependency_check.json"

    if not artifact.exists():
        result["runtime_error"] = True
        return result

    result["executed"] = True

    try:
        with open(artifact) as f:
            data = json.load(f)

        dependencies = data.get("dependencies", [])

        for dep in dependencies:
            vulnerabilities = dep.get("vulnerabilities", [])

            for vuln in vulnerabilities:
                severity = vuln.get("severity", "").lower()

                if severity == "critical":
                    normalized = "critical"
                elif severity == "high":
                    normalized = "high"
                elif severity == "medium":
                    normalized = "medium"
                elif severity == "low":
                    normalized = "low"
                else:
                    normalized = "info"

                result["severity_counts"][normalized] += 1
                result["violation_count"] += 1

        # Determine max severity
        if result["severity_counts"]["critical"] > 0:
            result["max_severity"] = "critical"
        elif result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result