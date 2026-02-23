# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/dependency_check.py
# ==========================================================
# dependency_check.py
# ----------------------------------------------------------
# OWASP Dependency-Check Parser (SCA)
#
# Purpose:
#   Parse the OWASP Dependency-Check JSON report and normalize
#   vulnerabilities into the standard schema (schemas.py).
#
# Expected raw artifact:
#   quality/analysis/raw/dependency_check.json
#
# Behavior:
#   - Iterates all dependencies and their vulnerability lists.
#   - Counts each vulnerability as one violation.
#   - Maps tool-provided severities to the normalized vocabulary:
#       critical | high | medium | low | info
#   - Does NOT apply enforcement thresholds (policy.yaml controls that).
#
# Policy linkage:
#   - policy.yaml can enforce strict rules such as:
#       any_vulnerability: true   (block if violation_count > 0)
#   - Future enhancements could support CVSS thresholds.
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_dependency_check(raw_dir: Path):
    """
    Parse Dependency-Check JSON and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - Parsing exceptions = runtime_error with error metadata.
    """
    tool_name = "dependency_check"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "dependency_check.json"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing artifact means the tool didn't run, failed before
    # writing output, or workflow wiring is incorrect.
    # This is treated as a runtime error and will block if the
    # tool is configured as blocking in policy.yaml.
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists; parser will attempt to read it)
    result["executed"] = True

    try:
        # Load JSON report
        with open(artifact) as f:
            data = json.load(f)

        # Dependency-Check JSON structure includes a "dependencies" array.
        dependencies = data.get("dependencies", [])

        # ------------------------------------------------------
        # Extract vulnerabilities
        # ------------------------------------------------------
        # Each dependency may include:
        #   dep["vulnerabilities"] = [ { "severity": "...", ... }, ... ]
        #
        # We count each vulnerability as one violation.
        # ------------------------------------------------------
        for dep in dependencies:
            vulnerabilities = dep.get("vulnerabilities", [])

            for vuln in vulnerabilities:
                severity = vuln.get("severity", "").lower()

                # Normalize dependency-check severity into our shared vocabulary.
                # If severity is missing/unknown, downgrade to "info" so it is still counted.
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

                # Update counters
                result["severity_counts"][normalized] += 1
                result["violation_count"] += 1

        # ------------------------------------------------------
        # Determine max severity encountered
        # ------------------------------------------------------
        # This enables simple severity-based enforcement rules
        # in policy_engine.py.
        # ------------------------------------------------------
        if result["severity_counts"]["critical"] > 0:
            result["max_severity"] = "critical"
        elif result["severity_counts"]["high"] > 0:
            result["max_severity"] = "high"
        elif result["severity_counts"]["medium"] > 0:
            result["max_severity"] = "medium"
        elif result["severity_counts"]["low"] > 0:
            result["max_severity"] = "low"
        else:
            # No findings
            result["max_severity"] = None

    except Exception as e:
        # Any exception parsing the artifact is treated as a runtime_error.
        # This includes malformed JSON or unexpected schema differences.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result