"""
TruffleHog Parser (Secrets Detection)

Purpose
-------
Parse TruffleHog JSONL output and normalize findings into the
standard schema defined in schemas.py.

Expected Raw Artifact
---------------------
quality/analysis/raw/trufflehog.jsonl

Native TruffleHog Severity Model
--------------------------------
TruffleHog does not use a traditional severity scale.
Instead, each finding carries a Verified boolean indicating whether
the secret was confirmed active against its target service.

Verified = true
    Secret is confirmed active and represents a live credential.
Verified = false
    Secret pattern matched but was not confirmed active.

Severity Mapping
----------------
TruffleHog -> Normalized

- Verified = true -> critical
- Verified = false -> high

Behavior
--------
- Reads a JSONL artifact with one JSON object per line.
- Skips blank lines and self-referential findings.
- Maps verified status to normalized severity.
- Populates findings with per-secret detail.
- Never writes raw secret values to findings or metadata.
- Counts violations per normalized severity level.
- Sets max_severity to the highest normalized severity found.
- Counts malformed JSONL lines and records them in metadata.
- Does not apply policy thresholds.

TruffleHog JSONL Record Structure
---------------------------------
{
  "DetectorName": "Github",
  "Verified": false,
  "Raw": "<secret>",
  "SourceMetadata": {
    "Data": {
      "Filesystem": {
        "file": "/repo/path/to/file.py",
        "line": 42
      }
    }
  },
  "ExtraData": {
    "rotation_guide": "https://..."
  }
}

Security Note
-------------
The Raw field contains the actual secret value.
It must never appear in findings, metadata, or any output artifact.
"""

import json
from pathlib import Path

from quality.ci.gate.schemas import base_tool_result
from quality.ci.gate.utils import determine_max_severity


def parse_trufflehog(raw_dir: Path) -> dict:
    """
    Parse TruffleHog JSONL and return a standardized result dictionary.

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
    - Never writes raw secret values to any output field.
    - Missing artifact sets artifact_present=False and runtime_error=True.
    - Malformed lines are counted in metadata and parsing continues.
    - Empty file is treated as a valid execution with zero violations.
    """
    tool_name = "trufflehog"
    result = base_tool_result(tool_name)
    artifact = raw_dir / "trufflehog.jsonl"

    if not artifact.exists():
        result["artifact_present"] = False
        result["runtime_error"] = True
        result["metadata"]["error"] = "Missing artifact: trufflehog.jsonl"
        return result

    result["artifact_present"] = True

    try:
        lines = artifact.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as error:
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to read trufflehog.jsonl: {error}"
        return result

    result["executed"] = True
    findings = []
    malformed_count = 0

    for raw_line in lines:
        raw_line = raw_line.strip()

        if not raw_line:
            continue

        try:
            rec = json.loads(raw_line)
        except json.JSONDecodeError:
            malformed_count += 1
            continue

        fs = (rec.get("SourceMetadata") or {}).get("Data", {}).get("Filesystem", {})
        file_path = _repo_relpath(fs.get("file"))
        line_no = fs.get("line")

        if file_path and file_path.endswith("quality/analysis/raw/trufflehog.jsonl"):
            continue

        detector = rec.get("DetectorName") or "TruffleHog"
        verified = bool(rec.get("Verified", False))
        normalized_severity = "critical" if verified else "high"
        result["severity_counts"][normalized_severity] += 1

        extra = rec.get("ExtraData") or {}
        rotation_guide = (
            extra.get("rotation_guide") if isinstance(extra, dict) else None
        )

        message = (
            f"{detector} secret detected ({'VERIFIED' if verified else 'unverified'})"
        )

        finding = {
            "severity": normalized_severity,
            "native_severity": "verified" if verified else "unverified",
            "rule": detector,
            "message": message,
            "file": file_path or "unknown",
            "line": line_no if line_no is not None else 0,
            "rule_url": rotation_guide or "",
        }
        findings.append(finding)

    result["findings"] = findings
    result["violation_count"] = len(findings)
    result["max_severity"] = determine_max_severity(result["severity_counts"])

    if malformed_count:
        result["metadata"]["malformed_lines"] = malformed_count

    return result


def _repo_relpath(path: str | None) -> str | None:
    """
    Convert an absolute TruffleHog container path to a repository-relative path.

    TruffleHog commonly runs inside Docker with the repository mounted at /repo.
    Paths in the output are therefore often prefixed with /repo/ and should be
    converted to repository-relative paths for downstream reporting.

    Parameters
    ----------
    path : str | None
        Absolute path from TruffleHog output, or None.

    Returns
    -------
    str | None
        Repository-relative path, or None if the input is empty.
    """
    if not path:
        return None

    if path.startswith("/repo/"):
        return path[len("/repo/"):]

    return path
