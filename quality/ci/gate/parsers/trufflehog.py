# File: quality/ci/gate/parsers/trufflehog.py
# ==========================================================
# trufflehog.py
# ----------------------------------------------------------
# TruffleHog Parser (Secrets Detection)
#
# Purpose:
#   Parse TruffleHog JSONL output and normalize findings into
#   the standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/trufflehog.jsonl
#
# Native TruffleHog Severity Model:
#   TruffleHog does not use a traditional severity scale.
#   Instead, each finding carries a "Verified" boolean that
#   indicates whether the secret was confirmed active against
#   its target service.
#
#   Verified=true  → Secret is confirmed active (live credential).
#   Verified=false → Secret pattern matched but not confirmed active.
#
# Severity Mapping (TruffleHog → Normalized):
#   Verified=true  → critical  (confirmed active credential)
#   Verified=false → high      (unverified but flagged secret)
#
# Behavior:
#   - Reads JSONL artifact (one JSON object per line).
#   - Skips blank lines and self-referential findings
#     (TruffleHog scanning its own output file).
#   - Maps verified status to normalized severity per the table above.
#   - Populates findings[] with per-secret detail.
#   - NEVER writes raw secret values to findings or metadata.
#   - Counts violations per normalized severity level.
#   - Sets max_severity to the highest normalized severity found.
#   - Malformed JSONL lines are counted and recorded in metadata.
#   - Does NOT apply policy thresholds (policy.yaml controls that).
#
# TruffleHog JSONL Record Structure:
#   {
#     "DetectorName": "Github",
#     "Verified": false,
#     "Raw": "<secret>",          ← NEVER output this field
#     "SourceMetadata": {
#       "Data": {
#         "Filesystem": {
#           "file": "/repo/path/to/file.py",
#           "line": 42
#         }
#       }
#     },
#     "ExtraData": {
#       "rotation_guide": "https://..."
#     }
#   }
#
# SECURITY NOTE:
#   The "Raw" field contains the actual secret value.
#   It MUST NOT appear in findings, metadata, or any output artifact.
# ==========================================================

import json
from pathlib import Path

from ..schemas import base_tool_result
from ..utils import determine_max_severity


def parse_trufflehog(raw_dir: Path) -> dict:
    """
    Parse TruffleHog JSONL and return a standardized result dictionary.

    Args:
        raw_dir: Path to the directory containing raw tool outputs.

    Returns:
        A dict conforming to the base_tool_result schema, populated
        with findings, severity counts, and max_severity.

    Contract:
        - Always returns a base_tool_result structure.
        - Never raises exceptions outward.
        - Never writes raw secret values to any output field.
        - Missing artifact → artifact_present=False, runtime_error=True.
        - Malformed lines  → counted in metadata, parsing continues.
        - Empty file       → valid result with zero violations.
    """
    tool_name = "trufflehog"

    # Initialize the standardized result structure
    result = base_tool_result(tool_name)

    # Build the expected artifact path
    artifact = raw_dir / "trufflehog.jsonl"

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
        result["metadata"]["error"] = "Missing artifact: trufflehog.jsonl"
        return result

    # Artifact is present; mark accordingly
    result["artifact_present"] = True

    try:
        # Read all lines from the JSONL artifact
        lines = artifact.read_text(encoding="utf-8", errors="replace").splitlines()
    except Exception as e:
        # Could not read the file at all — treat as runtime error
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to read trufflehog.jsonl: {e}"
        return result

    # Mark as executed — artifact was present and readable
    result["executed"] = True

    # Accumulate findings and track malformed lines
    findings       = []
    malformed_count = 0

    for raw_line in lines:
        raw_line = raw_line.strip()

        # Skip blank lines (common in JSONL files)
        if not raw_line:
            continue

        try:
            rec = json.loads(raw_line)
        except json.JSONDecodeError:
            # Malformed line — count it and continue processing remaining lines.
            # We do not abort on a single bad line; partial results are better
            # than no results.
            malformed_count += 1
            continue

        # ----------------------------------------------------------
        # Extract source location from SourceMetadata
        # ----------------------------------------------------------
        # TruffleHog filesystem scans nest file/line under:
        #   SourceMetadata → Data → Filesystem
        # ----------------------------------------------------------
        fs        = (rec.get("SourceMetadata") or {}).get("Data", {}).get("Filesystem", {})
        file_path = _repo_relpath(fs.get("file"))
        line_no   = fs.get("line")

        # ----------------------------------------------------------
        # Skip self-referential findings
        # ----------------------------------------------------------
        # TruffleHog may scan its own JSONL output and detect patterns
        # within previously written findings. These are false positives
        # and must be excluded.
        # ----------------------------------------------------------
        if file_path and file_path.endswith("quality/analysis/raw/trufflehog.jsonl"):
            continue

        # Extract detector name — used as the rule identifier
        detector = rec.get("DetectorName") or "TruffleHog"

        # Verified=true means the secret was confirmed active against its service
        verified = bool(rec.get("Verified", False))

        # ----------------------------------------------------------
        # Severity mapping
        # ----------------------------------------------------------
        # Verified secrets are confirmed live credentials → critical.
        # Unverified secrets are pattern matches only → high.
        # ----------------------------------------------------------
        normalized_severity = "critical" if verified else "high"

        # Increment the appropriate severity bucket directly on the
        # result dict — base_tool_result already owns the structure.
        result["severity_counts"][normalized_severity] += 1

        # Extract rotation guide URL from ExtraData if present
        extra          = rec.get("ExtraData") or {}
        rotation_guide = extra.get("rotation_guide") if isinstance(extra, dict) else None

        # Build human-readable message — verified status is explicit
        message = f"{detector} secret detected ({'VERIFIED' if verified else 'unverified'})"

        # ----------------------------------------------------------
        # Build the standardized finding record.
        # SECURITY: The "Raw" field from the TruffleHog record
        # is intentionally excluded — it contains the actual secret.
        # ----------------------------------------------------------
        finding = {
            "severity":        normalized_severity,
            "native_severity": "verified" if verified else "unverified",
            "rule":            detector,
            "message":         message,
            "file":            file_path or "unknown",
            "line":            line_no if line_no is not None else 0,
            "rule_url":        rotation_guide or "",
        }
        findings.append(finding)

    # Store all individual findings
    result["findings"] = findings

    # Total number of secrets found
    result["violation_count"] = len(findings)

    # Determine max_severity using the shared utility function
    result["max_severity"] = determine_max_severity(result["severity_counts"])

    # Record malformed line count in metadata if any were encountered
    if malformed_count:
        result["metadata"]["malformed_lines"] = malformed_count

    return result


def _repo_relpath(path: str | None) -> str | None:
    """
    Convert an absolute TruffleHog container path to a repo-relative path.

    TruffleHog runs inside Docker with the repository mounted at /repo.
    All file paths in the output are prefixed with /repo/ and must be
    stripped to produce paths that are meaningful in the repository context.

    Args:
        path: Absolute path string from TruffleHog output, or None.

    Returns:
        Repo-relative path string, or None if input was None/empty.
    """
    if not path:
        return None

    # Strip the Docker bind mount prefix used in the CI workflow
    if path.startswith("/repo/"):
        return path[len("/repo/"):]

    # Return as-is if the prefix is not present (local run or different mount)
    return path
