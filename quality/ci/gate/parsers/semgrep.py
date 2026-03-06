"""
Semgrep Parser (Multi-language SAST)

Purpose
-------
Parse Semgrep JSON output and normalize findings into the
standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/semgrep.json

Native Semgrep Severities
-------------------------
ERROR
    Rule matched a high-confidence security issue.
WARNING
    Rule matched a potential issue with lower confidence.
INFO
    Informational match at the lowest enforcement level.
INVENTORY
    Inventory or audit finding rather than a direct vulnerability.

Severity Mapping
----------------
Semgrep -> Normalized

- ERROR -> high
- WARNING -> medium
- INFO -> low
- INVENTORY -> info
- unknown -> info

Note
----
Semgrep does not emit a native critical severity. High is the
maximum mapped level.

Behavior
--------
- Reads the "results" array from the Semgrep JSON artifact.
- Maps native severity to normalized severity.
- Populates findings with per-finding detail including CWE and OWASP.
- Counts violations per normalized severity level.
- Sets max_severity to the highest normalized severity found.
- Does not apply policy thresholds.

Semgrep JSON Structure
----------------------
{
  "results": [
    {
      "check_id": "python.flask.security.injection.tainted-sql-string",
      "path": "src/main/app.py",
      "start": { "line": 42, "col": 5 },
      "end": { "line": 42, "col": 30 },
      "extra": {
        "severity": "ERROR",
        "message": "Possible SQL injection...",
        "metadata": {
          "cwe": ["CWE-89: Improper Neutralization..."],
          "owasp": ["A1:2017 - Injection"]
        }
      }
    }
  ]
}
"""

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


SEVERITY_MAP = {
    "error": "high",
    "warning": "medium",
    "info": "low",
    "inventory": "info",
}


def parse_semgrep(raw_dir: Path) -> dict:
    """
    Parse Semgrep JSON and return a standardized result dictionary.

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
    - Malformed JSON sets runtime_error=True and records the error in metadata.
    - Empty results are treated as a valid execution with zero violations.
    """
    tool_name = "semgrep"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "semgrep.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        return result

    result["artifact_present"] = True
    result["executed"] = True

    try:
        with open(artifact, "r", encoding="utf-8") as f:
            data = json.load(f)

        raw_findings = data.get("results", [])
        findings = []

        for raw in raw_findings:
            extra = raw.get("extra", {})
            metadata = extra.get("metadata", {})

            native_severity = extra.get("severity", "INFO").lower()
            normalized_severity = SEVERITY_MAP.get(native_severity, "info")
            result["severity_counts"][normalized_severity] += 1

            start = raw.get("start", {})
            cwe = metadata.get("cwe", [])
            owasp = metadata.get("owasp", [])

            if isinstance(cwe, str):
                cwe = [cwe]
            if isinstance(owasp, str):
                owasp = [owasp]

            finding = {
                "file": raw.get("path", "unknown"),
                "line": start.get("line", 0),
                "column": start.get("col", 0),
                "severity": normalized_severity,
                "native_severity": native_severity.upper(),
                "rule": raw.get("check_id", "unknown"),
                "message": extra.get("message", ""),
                "cwe": cwe,
                "owasp": owasp,
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
