# ==========================================================
# sonarqube.py
# ----------------------------------------------------------
# Parses SonarQube Quality Gate JSON response.
#
# Expected raw artifact:
# quality/analysis/raw/sonarqube.json
#
# Expected structure:
# {
#   "projectStatus": {
#       "status": "OK" | "ERROR"
#   }
# }
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_sonarqube(raw_dir: Path):
    tool_name = "sonarqube"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "sonarqube.json"

    if not artifact.exists():
        # Sonar often optional — treat missing as runtime error
        result["runtime_error"] = True
        return result

    result["executed"] = True

    try:
        with open(artifact) as f:
            data = json.load(f)

        status = (
            data.get("projectStatus", {})
            .get("status", "")
            .upper()
        )

        if status == "ERROR":
            result["violation_count"] = 1
            result["severity_counts"]["high"] = 1
            result["max_severity"] = "high"
        elif status == "OK":
            result["violation_count"] = 0
            result["max_severity"] = None
        else:
            # Unexpected response
            result["runtime_error"] = True
            result["metadata"]["error"] = f"Unknown Sonar status: {status}"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result