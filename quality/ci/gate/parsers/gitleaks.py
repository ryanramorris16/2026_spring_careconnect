"""
Gitleaks Parser (Secrets Detection)

Purpose
-------
Parse Gitleaks JSON output and normalize findings into the
standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/gitleaks.json

Native Gitleaks Severity Model
------------------------------
Gitleaks does not emit a severity field in its output.
Severity is inferred from the rule's tags array when present,
or defaulted based on the presence of a detected secret.

Severity Inference Rules
------------------------
- Tag "critical" present -> critical
- Tag "high" present -> high
- Tag "medium" present -> medium
- Tag "low" present -> low
- No severity tag -> high

Behavior
--------
- Reads a JSON array artifact with one object per finding.
- Skips self-referential findings where Gitleaks scans its own output.
- Maps rule tags to normalized severity.
- Populates findings with per-secret detail.
- Never writes raw secret values from Secret or Match fields.
- Counts violations per normalized severity level.
- Sets max_severity to the highest normalized severity found.
- Handles both empty array and null artifact content gracefully.
- Does not apply policy thresholds.

Gitleaks JSON Record Structure
------------------------------
[
  {
    "Description": "AWS Access Key",
    "StartLine": 42,
    "EndLine": 42,
    "StartColumn": 1,
    "EndColumn": 20,
    "Match": "AKIA...",
    "Secret": "AKIA...",
    "File": "path/to/file.env",
    "SymlinkFile": "",
    "Commit": "",
    "Entropy": 3.5,
    "Author": "",
    "Email": "",
    "Date": "",
    "Message": "",
    "Tags": ["aws", "high"],
    "RuleID": "aws-access-key",
    "Fingerprint": "abc123"
  }
]

Security Note
-------------
The Secret and Match fields contain the actual secret value.
They must never appear in findings, metadata, or any output artifact.
"""

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


_SEVERITY_TAGS = ["critical", "high", "medium", "low"]
_DEFAULT_SEVERITY = "high"


def parse_gitleaks(raw_dir: Path) -> dict:
    """
    Parse Gitleaks JSON and return a standardized result dictionary.

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
    - Never writes raw secret values from Secret or Match.
    - Missing artifact sets artifact_present=False and runtime_error=True.
    - Empty array is treated as a valid execution with zero violations.
    - Malformed JSON sets runtime_error=True and records the error in metadata.
    """
    tool_name = "gitleaks"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "gitleaks.json"

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

    if records is None:
        records = []

    if not isinstance(records, list):
        result["runtime_error"] = True
        result["metadata"]["error"] = (
            "Unexpected gitleaks.json structure: expected array, "
            f"got {type(records).__name__}"
        )
        return result

    result["executed"] = True
    findings = []

    for rec in records:
        if not isinstance(rec, dict):
            continue

        file_path = rec.get("File") or rec.get("SymlinkFile") or "unknown"

        if file_path and file_path.endswith("quality/analysis/raw/gitleaks.json"):
            continue

        line_no = rec.get("StartLine") or 0
        rule_id = rec.get("RuleID") or "gitleaks"
        desc = rec.get("Description") or rule_id
        tags = [str(tag).lower() for tag in (rec.get("Tags") or [])]

        normalized_severity = _DEFAULT_SEVERITY
        for level in _SEVERITY_TAGS:
            if level in tags:
                normalized_severity = level
                break

        result["severity_counts"][normalized_severity] += 1

        entropy = rec.get("Entropy")
        if entropy is not None:
            try:
                message = f"{desc} (entropy: {float(entropy):.2f})"
            except (TypeError, ValueError):
                message = desc
        else:
            message = desc

        finding = {
            "severity": normalized_severity,
            "native_severity": ",".join(tags) if tags else "none",
            "rule": rule_id,
            "message": message,
            "file": file_path,
            "line": line_no,
            "rule_url": "",
        }
        findings.append(finding)

    result["findings"] = findings
    result["violation_count"] = len(findings)
    result["max_severity"] = determine_max_severity(result["severity_counts"])

    return result
