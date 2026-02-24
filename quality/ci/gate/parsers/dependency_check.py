# File: quality/ci/gate/parsers/dependency_check.py
# ==========================================================
# dependency_check.py
# ----------------------------------------------------------
# OWASP Dependency-Check Parser (SCA - Software Composition Analysis)
#
# Purpose:
#   Parse the OWASP Dependency-Check JSON report and normalize
#   vulnerabilities into the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/dependency-check-report.json
#
# Native Dependency-Check Severities:
#   CRITICAL      → Typically CVSS v3 score 9.0–10.0
#   HIGH          → Typically CVSS v3 score 7.0–8.9
#   MEDIUM        → Typically CVSS v3 score 4.0–6.9
#   LOW           → Typically CVSS v3 score 0.1–3.9
#   INFORMATIONAL → Informational only, no CVSS score or unscored
#
# Severity Mapping (Dependency-Check → Normalized):
#   CRITICAL      → critical
#   HIGH          → high
#   MEDIUM        → medium
#   LOW           → low
#   INFORMATIONAL → info
#   <unknown>     → info
#
# Behavior:
#   - Iterates all dependencies and their vulnerability lists.
#   - Counts each vulnerability as one violation.
#   - Populates findings[] with per-vulnerability detail.
#   - Prefers CVSS v3 score; falls back to CVSS v2 if v3 is absent.
#   - Sets max_severity to the highest normalized severity found.
#   - Does NOT apply enforcement thresholds (policy.yaml controls that).
#
# Dependency-Check JSON Structure:
#   {
#     "dependencies": [
#       {
#         "fileName": "spring-core-5.3.18.jar",
#         "packages": [ { "id": "pkg:maven/..." } ],
#         "vulnerabilities": [
#           {
#             "name": "CVE-2022-22965",
#             "severity": "CRITICAL",
#             "description": "...",
#             "cwes": ["CWE-94"],
#             "cvssv3": {
#               "baseScore": 9.8,
#               "attackVector": "NETWORK"
#             },
#             "cvssv2": { "score": 7.5 },
#             "references": [
#               {
#                 "name": "NVD",
#                 "url": "https://nvd.nist.gov/vuln/detail/CVE-2022-22965"
#               }
#             ]
#           }
#         ]
#       }
#     ]
#   }
#
# NVD API Key:
#   OWASP Dependency-Check uses the NVD API to fetch vulnerability data.
#   Without a key, requests are rate-limited and scans may be slow or fail.
#
#   Once you receive your NVD API key:
#     Step 1 — Add it as a GitHub Actions secret:
#       Name:  NVD_API_KEY
#       Value: <your key from https://nvd.nist.gov/developers/request-an-api-key>
#
#     Step 2 — Pass it to the Dependency-Check CLI in the workflow step:
#       --nvdApiKey "${{ secrets.NVD_API_KEY }}"
#
#     Step 3 — Remove --noupdate from the workflow step (if present),
#       as it prevents the NVD database from being updated.
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


# ----------------------------------------------------------
# Severity mapping: Dependency-Check native → normalized
# ----------------------------------------------------------
SEVERITY_MAP = {
    "critical":      "critical",
    "high":          "high",
    "medium":        "medium",
    "low":           "low",
    "informational": "info",
}


def parse_dependency_check(raw_dir: Path) -> dict:
    """
    Parse Dependency-Check JSON and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema, populated
        with findings, severity counts, and max_severity.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed JSON   → runtime_error=True, error captured in metadata.
    """
    tool_name = "dependency_check"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "dependency-check-report.json"

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
        # Load and parse the JSON report
        with open(artifact) as f:
            data = json.load(f)

        # Top-level "dependencies" array contains all scanned libraries
        dependencies = data.get("dependencies", [])

        # Accumulate findings as we walk the dependency tree
        findings = []

        for dep in dependencies:
            # Extract the file name of the dependency (e.g. spring-core-5.3.18.jar)
            file_name = dep.get("fileName", "unknown")

            # Extract the package URL (purl) from the packages list if present.
            # purl format: pkg:maven/org.springframework/spring-core@5.3.18
            packages   = dep.get("packages", [])
            package_id = packages[0].get("id", "unknown") if packages else "unknown"

            # Each dependency may have zero or more associated vulnerabilities
            vulnerabilities = dep.get("vulnerabilities", [])

            for vuln in vulnerabilities:
                # CVE identifier (also used as rule for schema consistency)
                cve = vuln.get("name", "unknown")

                # Native severity as emitted by the tool (e.g. "CRITICAL")
                native_severity = vuln.get("severity", "INFORMATIONAL")

                # Normalize to lowercase for map lookup; default to info
                # if the value is missing or unrecognized
                normalized_severity = SEVERITY_MAP.get(
                    native_severity.lower(), "info"
                )

                # Prefer CVSS v3 base score; fall back to CVSS v2 score
                cvss_v3     = vuln.get("cvssv3", {})
                cvss_v2     = vuln.get("cvssv2", {})
                cvss_score  = cvss_v3.get("baseScore") or cvss_v2.get("score")

                # CVSS v3 attack vector describes the exploitation context
                # (e.g. NETWORK, ADJACENT, LOCAL, PHYSICAL)
                cvss_vector = cvss_v3.get("attackVector", "")

                # CWE identifiers associated with this vulnerability
                # Dependency-Check emits a list e.g. ["CWE-94", "CWE-20"]
                cwes = vuln.get("cwes", [])
                if isinstance(cwes, str):
                    cwes = [cwes]

                # Extract all reference URLs for audit trail
                references = vuln.get("references", [])

                # Pull the NVD reference URL directly for quick access
                nvd_url = ""
                for ref in references:
                    if isinstance(ref, dict) and ref.get("name", "").upper() == "NVD":
                        nvd_url = ref.get("url", "")
                        break

                # Truncate description to 500 characters to keep the report
                # readable and avoid bloating the normalized output
                description = vuln.get("description", "")
                if len(description) > 500:
                    description = description[:500] + "..."

                # Increment the appropriate severity bucket on the result dict.
                # base_tool_result already owns the full five-level structure.
                result["severity_counts"][normalized_severity] += 1

                # Build the standardized finding record
                finding = {
                    "file":            file_name,
                    "package":         package_id,
                    "cve":             cve,
                    "rule":            cve,           # Kept consistent with schema convention
                    "severity":        normalized_severity,
                    "native_severity": native_severity,
                    "cvss_score":      cvss_score,
                    "cvss_vector":     cvss_vector,
                    "cwe":             cwes,
                    "nvd_url":         nvd_url,
                    "references":      references,
                    "description":     description,
                }
                findings.append(finding)

        # Store all individual findings
        result["findings"] = findings

        # Total number of vulnerabilities found across all dependencies
        result["violation_count"] = len(findings)

        # Determine max_severity using the shared utility function
        result["max_severity"] = determine_max_severity(result["severity_counts"])

    except json.JSONDecodeError as e:
        # ----------------------------------------------------------
        # Malformed or unparseable JSON.
        # Captured separately from generic exceptions so the error
        # type is explicit in the metadata.
        # ----------------------------------------------------------
        result["runtime_error"] = True
        result["metadata"]["error"] = f"JSON parse error: {e}"

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