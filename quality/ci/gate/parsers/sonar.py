"""
SonarQube / SonarCloud Quality Gate Parser

Purpose
-------
Parse the Sonar Quality Gate JSON artifact and normalize findings
into the standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/sonar.json

Native Sonar Severities
-----------------------
BLOCKER
    Must-fix issue. Application behavior is critically affected.
CRITICAL
    High-impact issue. Often security or reliability related.
MAJOR
    Significant issue that may affect productivity or reliability.
MINOR
    Smaller issue with limited application impact.
INFO
    Informational only and lowest enforcement level.

Severity Mapping
----------------
Sonar -> Normalized

- BLOCKER -> critical
- CRITICAL -> high
- MAJOR -> medium
- MINOR -> low
- INFO -> info
- unknown -> info

Current Status
--------------
This tool is currently inactive and operates in placeholder mode.

The CI workflow writes a placeholder artifact to satisfy the gate engine:

    {"projectStatus": {"status": "OK"}, "issues": []}

The parser detects the disabled state and marks the tool as disabled.
A disabled Sonar tool does not count as a runtime error and does not
block the merge regardless of policy configuration.

Activation Steps
----------------
Step 1
    Create a SonarCloud account:
    https://sonarcloud.io

Step 2
    Add the following GitHub Actions secrets:
    SONAR_TOKEN
        SonarCloud authentication token
    SONAR_PROJECT_KEY
        Project key from SonarCloud
    SONAR_ORG
        SonarCloud organization name

Step 3
    Replace the placeholder CI step in build-and-analyze.yml
    with a real Sonar scan step using:
    uses: SonarSource/sonarcloud-github-action@master

Step 4
    Update the CI step to write the real Sonar API response:
    curl -u "${SONAR_TOKEN}:" \
      "https://sonarcloud.io/api/qualitygates/project_status?projectKey=${SONAR_PROJECT_KEY}" \
      > quality/analysis/raw/sonar.json

Step 5
    Set SONAR_ENABLED = True in this file.

Step 6
    Update policy.yaml to set tools.sonar.blocking = true.

Behavior When Active
--------------------
- Reads projectStatus.status for the overall gate result.
- Reads issues for per-finding detail.
- Maps native Sonar severities to normalized severities.
- Populates findings with per-issue detail.
- Sets max_severity to the highest normalized severity found.
- Does not apply policy thresholds.

Sonar JSON Structure
--------------------
{
  "projectStatus": {
    "status": "ERROR"
  },
  "issues": [
    {
      "severity": "CRITICAL",
      "message": "Possible null pointer dereference.",
      "component": "com.careconnect.auth:LoginService.java",
      "line": 42,
      "rule": "java:S2259"
    }
  ]
}
"""

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


SONAR_ENABLED = False


SEVERITY_MAP = {
    "blocker": "critical",
    "critical": "high",
    "major": "medium",
    "minor": "low",
    "info": "info",
}


def parse_sonar(raw_dir: Path) -> dict:
    """
    Parse sonar.json and return a standardized result dictionary.

    Parameters
    ----------
    raw_dir : Path
        Directory containing raw tool output artifacts.

    Returns
    -------
    dict
        Result dictionary conforming to the base_tool_result schema.
        When the tool is disabled, returns a clean disabled result with
        zero violations and metadata describing the inactive state.

    Contract
    --------
    - Always returns a base_tool_result structure.
    - Never raises exceptions outward.
    - When SONAR_ENABLED is False, returns a disabled result immediately.
    - Missing artifact sets artifact_present=False and runtime_error=True.
    - Malformed JSON sets runtime_error=True and records the error in metadata.
    """
    tool_name = "sonar"
    result = base_tool_result(tool_name)

    if not SONAR_ENABLED:
        result["artifact_present"] = True
        result["executed"] = False
        result["runtime_error"] = False
        result["metadata"]["status"] = "disabled"
        result["metadata"]["reason"] = (
            "SonarQube/SonarCloud is not configured. "
            "See activation steps in sonar.py header comments."
        )
        return result

    artifact = raw_dir / "sonar.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        result["metadata"]["error"] = "Missing artifact: sonar.json"
        return result

    result["artifact_present"] = True
    result["executed"] = True

    try:
        with open(artifact, "r", encoding="utf-8") as f:
            data = json.load(f)

        project_status = data.get("projectStatus", {})
        gate_status = project_status.get("status", "UNKNOWN").strip().upper()
        result["metadata"]["gate_status"] = gate_status

        issues = data.get("issues", [])
        findings = []

        for issue in issues:
            native_severity = issue.get("severity", "INFO").lower()
            normalized_severity = SEVERITY_MAP.get(native_severity, "info")
            result["severity_counts"][normalized_severity] += 1

            finding = {
                "file": issue.get("component", "unknown"),
                "line": issue.get("line", 0),
                "severity": normalized_severity,
                "native_severity": native_severity.upper(),
                "rule": issue.get("rule", "unknown"),
                "message": issue.get("message", ""),
            }
            findings.append(finding)

        result["findings"] = findings
        result["violation_count"] = len(findings)
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"JSON parse error: {e}"

    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Unexpected error: {e}"

    return result
