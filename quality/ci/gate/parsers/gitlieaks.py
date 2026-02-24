# File: quality/ci/gate/parsers/gitleaks.py
# ==========================================================
# gitleaks.py
# ----------------------------------------------------------
# Gitleaks Parser (Secrets Detection)
#
# Purpose:
#   Parse Gitleaks JSON output and normalize findings into
#   the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/gitleaks.json
#
# Native Gitleaks Severity Model:
#   Gitleaks does not emit a severity field in its output.
#   Severity is inferred from the rule's tags[] array when
#   present, or defaulted based on whether the match falls
#   within a high-entropy value (heuristic).
#
#   Tag "critical" present → critical
#   Tag "high"     present → high
#   Tag "medium"   present → medium
#   Tag "low"      present → low
#   No severity tag        → high  (secret detected = high by default)
#
# Behavior:
#   - Reads a JSON array artifact (one object per finding).
#   - Skips self-referential findings (Gitleaks scanning its own output).
#   - Maps rule tags to normalized severity per the table above.
#   - Populates findings[] with per-secret detail.
#   - NEVER writes raw secret values (Secret field) to findings or metadata.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Handles both empty array [] and null/missing artifact gracefully.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# Gitleaks JSON Record Structure:
#   [
#     {
#       "Description": "AWS Access Key",
#       "StartLine":   42,
#       "EndLine":     42,
#       "StartColumn": 1,
#       "EndColumn":   20,
#       "Match":       "AKIA...",     ← NEVER output this field
#       "Secret":      "AKIA...",     ← NEVER output this field
#       "File":        "path/to/file.env",
#       "SymlinkFile": "",
#       "Commit":      "",
#       "Entropy":     3.5,
#       "Author":      "",
#       "Email":       "",
#       "Date":        "",
#       "Message":     "",
#       "Tags":        ["aws", "high"],
#       "RuleID":      "aws-access-key",
#       "Fingerprint": "abc123"
#     }
#   ]
#
# SECURITY NOTE:
#   The "Secret" and "Match" fields contain the actual secret value.
#   They MUST NOT appear in findings, metadata, or any output artifact.
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity

# ----------------------------------------------------------
# Severity tag priority order (highest wins)
# ----------------------------------------------------------
_SEVERITY_TAGS = ["critical", "high", "medium", "low"]
_DEFAULT_SEVERITY = "high"


def parse_gitleaks(raw_dir: Path) -> dict:
    """
    Parse Gitleaks JSON and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema, populated
        with findings, severity counts, and max_severity.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Never writes raw secret values (Secret/Match) to any output field.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Empty array []  → valid result with zero violations.
        - Malformed JSON  → runtime_error=True, error recorded in metadata.
    """
    tool_name = "gitleaks"

    result = base_tool_result(tool_name)

    artifact = raw_dir / "gitleaks.json"

    # ----------------------------------------------------------
    # Artifact presence check
    # ----------------------------------------------------------
    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        result["metadata"]["error"] = "Missing artifact: gitleaks.json"
        return result

    result["artifact_present"] = True

    try:
        raw_text = artifact.read_text(encoding="utf-8", errors="replace").strip()
    except Exception as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to read gitleaks.json: {e}"
        return result

    # Gitleaks writes an empty file when no secrets are found in some
    # versions — treat as zero findings rather than a parse error.
    if not raw_text:
        result["executed"] = True
        result["findings"] = []
        result["violation_count"] = 0
        result["max_severity"] = None
        return result

    try:
        records = json.loads(raw_text)
    except json.JSONDecodeError as e:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to parse gitleaks.json: {e}"
        return result

    # Gitleaks outputs a JSON array; null means no findings in some versions
    if records is None:
        records = []

    if not isinstance(records, list):
        result["runtime_error"] = True
        result["metadata"]["error"] = (
            f"Unexpected gitleaks.json structure: expected array, got {type(records).__name__}"
        )
        return result

    result["executed"] = True

    findings = []

    for rec in records:
        if not isinstance(rec, dict):
            continue

        file_path = rec.get("File") or rec.get("SymlinkFile") or "unknown"

        # ----------------------------------------------------------
        # Skip self-referential findings
        # ----------------------------------------------------------
        if file_path and file_path.endswith("quality/analysis/raw/gitleaks.json"):
            continue

        line_no  = rec.get("StartLine") or 0
        rule_id  = rec.get("RuleID") or "gitleaks"
        desc     = rec.get("Description") or rule_id
        tags     = [t.lower() for t in (rec.get("Tags") or [])]

        # ----------------------------------------------------------
        # Severity inference from tags
        # ----------------------------------------------------------
        normalized_severity = _DEFAULT_SEVERITY
        for level in _SEVERITY_TAGS:
            if level in tags:
                normalized_severity = level
                break

        result["severity_counts"][normalized_severity] += 1

        # Build message — description + entropy hint when available
        entropy = rec.get("Entropy")
        if entropy is not None:
            message = f"{desc} (entropy: {entropy:.2f})"
        else:
            message = desc

        # ----------------------------------------------------------
        # Build the standardized finding record.
        # SECURITY: "Secret" and "Match" fields are intentionally
        # excluded — they contain the actual secret value.
        # ----------------------------------------------------------
        finding = {
            "severity":        normalized_severity,
            "native_severity": ",".join(tags) if tags else "none",
            "rule":            rule_id,
            "message":         message,
            "file":            file_path,
            "line":            line_no,
            "rule_url":        "",
        }
        findings.append(finding)

    result["findings"] = findings
    result["violation_count"] = len(findings)
    result["max_severity"] = determine_max_severity(result["severity_counts"])

    return result
