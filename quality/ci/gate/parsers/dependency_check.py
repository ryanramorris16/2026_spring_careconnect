"""
OWASP Dependency-Check Parser (Software Composition Analysis)

Purpose
-------
Parse the OWASP Dependency-Check JSON report and normalize
vulnerabilities into the standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/dependency-check-report.json

Native Dependency-Check Severities
----------------------------------
CRITICAL
    Typically CVSS v3 score 9.0–10.0
HIGH
    Typically CVSS v3 score 7.0–8.9
MEDIUM
    Typically CVSS v3 score 4.0–6.9
LOW
    Typically CVSS v3 score 0.1–3.9
INFORMATIONAL
    Informational only, no CVSS score or unscored

Severity Mapping
----------------
Dependency-Check -> Normalized

- CRITICAL -> critical
- HIGH -> high
- MEDIUM -> medium
- LOW -> low
- INFORMATIONAL -> info
- unknown -> info

Behavior
--------
- Iterates all dependencies and their vulnerability lists.
- Counts each vulnerability as one violation.
- Populates findings with per-vulnerability detail.
- Prefers CVSS v3 score and falls back to CVSS v2 if v3 is absent.
- Sets max_severity to the highest normalized severity found.
- Does not apply enforcement thresholds.

Dependency-Check JSON Structure
-------------------------------
{
  "dependencies": [
    {
      "fileName": "spring-core-5.3.18.jar",
      "packages": [ { "id": "pkg:maven/..." } ],
      "vulnerabilities": [
        {
          "name": "CVE-2022-22965",
          "severity": "CRITICAL",
          "description": "...",
          "cwes": ["CWE-94"],
          "cvssv3": {
            "baseScore": 9.8,
            "attackVector": "NETWORK"
          },
          "cvssv2": { "score": 7.5 },
          "references": [
            {
              "name": "NVD",
              "url": "https://nvd.nist.gov/vuln/detail/CVE-2022-22965"
            }
          ]
        }
      ]
    }
  ]
}

NVD API Key
-----------
OWASP Dependency-Check uses the NVD API to fetch vulnerability data.
Without a key, requests are rate-limited and scans may be slow or fail.

Once you receive your NVD API key:

Step 1
    Add it as a GitHub Actions secret:
    Name: NVD_API_KEY
    Value: <your key from https://nvd.nist.gov/developers/request-an-api-key>

Step 2
    Pass it to the Dependency-Check CLI in the workflow step:
    --nvdApiKey "${{ secrets.NVD_API_KEY }}"

Step 3
    Remove --noupdate from the workflow step, if present,
    because it prevents the NVD database from being updated.
"""

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


SEVERITY_MAP = {
    "critical": "critical",
    "high": "high",
    "medium": "medium",
    "low": "low",
    "informational": "info",
}


def parse_dependency_check(raw_dir: Path) -> dict:
    """
    Parse Dependency-Check JSON and return a standardized result dictionary.

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
    """
    tool_name = "dependency_check"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "dependency-check-report.json"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        return result

    result["artifact_present"] = True
    result["executed"] = True

    try:
        with open(artifact, "r", encoding="utf-8") as f:
            data = json.load(f)

        dependencies = data.get("dependencies", [])
        findings = []

        for dep in dependencies:
            file_name = dep.get("fileName", "unknown")
            packages = dep.get("packages", [])
            package_id = packages[0].get("id", "unknown") if packages else "unknown"
            vulnerabilities = dep.get("vulnerabilities", [])

            for vuln in vulnerabilities:
                cve = vuln.get("name", "unknown")
                native_severity = vuln.get("severity", "INFORMATIONAL")
                normalized_severity = SEVERITY_MAP.get(
                    native_severity.lower(),
                    "info",
                )

                cvss_v3 = vuln.get("cvssv3", {})
                cvss_v2 = vuln.get("cvssv2", {})
                cvss_score = cvss_v3.get("baseScore") or cvss_v2.get("score")
                cvss_vector = cvss_v3.get("attackVector", "")

                cwes = vuln.get("cwes", [])
                if isinstance(cwes, str):
                    cwes = [cwes]

                references = vuln.get("references", [])

                nvd_url = ""
                for ref in references:
                    if isinstance(ref, dict) and ref.get("name", "").upper() == "NVD":
                        nvd_url = ref.get("url", "")
                        break

                description = vuln.get("description", "")
                if len(description) > 500:
                    description = description[:500] + "..."

                result["severity_counts"][normalized_severity] += 1

                finding = {
                    "file": file_name,
                    "package": package_id,
                    "cve": cve,
                    "rule": cve,
                    "severity": normalized_severity,
                    "native_severity": native_severity,
                    "cvss_score": cvss_score,
                    "cvss_vector": cvss_vector,
                    "cwe": cwes,
                    "nvd_url": nvd_url,
                    "references": references,
                    "description": description,
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
