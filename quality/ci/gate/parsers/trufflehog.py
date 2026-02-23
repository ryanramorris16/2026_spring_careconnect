# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/trufflehog.py
# ==========================================================
# trufflehog.py
# ----------------------------------------------------------
# TruffleHog Parser (Secrets)
#
# Purpose:
#   Parse TruffleHog JSONL output and normalize results into the
#   standard schema defined in schemas.py, including per-finding
#   detail (message + file + line + rule_url) so humanize.py can
#   generate drill-down pages with snippets.
#
# Expected raw artifact:
#   quality/analysis/raw/trufflehog.jsonl
#
# TruffleHog JSONL record sample (from your CI):
#   {
#     "SourceMetadata": {"Data": {"Filesystem": {"file":"/repo/...","line":123}}},
#     "DetectorName":"Github",
#     "DetectorDescription":"...",
#     "Verified": false,
#     "Raw":"<secret>",
#     "ExtraData":{"rotation_guide":"https://..."}
#   }
#
# Normalization Rules:
#   - NEVER output the raw secret value ("Raw") into findings.
#   - Findings include:
#       severity="high" (secrets are high by default in this engine)
#       message="<DetectorName> secret detected (verified/unverified)"
#       file="<repo-relative path>"
#       line=<line number>
#       rule="<DetectorName>"
#       rule_url="<rotation_guide if present>"
#
# Governance Notes:
#   - Missing artifact => artifact_present=False, executed=False, runtime_error=True
#   - Malformed JSONL line => record a finding with metadata and continue
#   - Parsers must NOT apply policy thresholds (policy.yaml owns enforcement).
# ==========================================================

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Optional

from ..schemas import base_tool_result


def _repo_relpath(p: Optional[str]) -> Optional[str]:
    """
    Convert TruffleHog filesystem paths like:
      /repo/frontend/lib/x.dart
    into repo-relative paths:
      frontend/lib/x.dart
    """
    if not p:
        return None

    # Trufflehog docker bind mount uses /repo as container root.
    if p.startswith("/repo/"):
        return p[len("/repo/") :]

    # Fallback: return as-is (still may work for snippet reads if relative)
    return p


def parse_trufflehog(raw_dir: Path) -> Dict[str, Any]:
    tool_name = "trufflehog"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "trufflehog.jsonl"

    # Artifact present?
    if artifact.exists():
        result["artifact_present"] = True
    else:
        result["artifact_present"] = False
        result["executed"] = False
        result["runtime_error"] = True
        result["metadata"]["error"] = "Missing artifact: trufflehog.jsonl"
        return result

    # We have an artifact; attempt to parse.
    try:
        lines = artifact.read_text(encoding="utf-8", errors="replace").splitlines()
    except Exception as e:
        result["executed"] = False
        result["runtime_error"] = True
        result["metadata"]["error"] = f"Failed to read trufflehog.jsonl: {e}"
        return result

    findings = []
    malformed_count = 0

    for raw_line in lines:
        raw_line = raw_line.strip()
        if not raw_line:
            continue

        try:
            rec = json.loads(raw_line)
        except Exception:
            malformed_count += 1
            continue

        detector = rec.get("DetectorName") or "TruffleHog"
        verified = bool(rec.get("Verified", False))

        # Extract file + line from Filesystem metadata
        fs = (
            (rec.get("SourceMetadata") or {})
            .get("Data", {})
            .get("Filesystem", {})
        )
        file_path = _repo_relpath(fs.get("file"))
        line_no = fs.get("line")

        # Filter out self-referential findings (trufflehog scanning its own output)
        # Example in your data: /repo/quality/analysis/raw/trufflehog.jsonl
        if file_path and file_path.endswith("quality/analysis/raw/trufflehog.jsonl"):
            continue

        rotation_guide = None
        extra = rec.get("ExtraData") or {}
        if isinstance(extra, dict):
            rotation_guide = extra.get("rotation_guide")

        # IMPORTANT: do NOT include rec["Raw"] in any output
        msg = f"{detector} secret detected ({'VERIFIED' if verified else 'unverified'})"

        finding: Dict[str, Any] = {
            "severity": "high",
            "message": msg,
            "rule": detector,
        }

        if rotation_guide:
            finding["rule_url"] = rotation_guide

        if file_path:
            finding["file"] = file_path

        # line can be missing or non-int; store as-is and let humanize.py safe-cast
        if line_no is not None:
            finding["line"] = line_no

        findings.append(finding)

    # Populate standardized fields
    result["executed"] = True
    result["runtime_error"] = False
    result["findings"] = findings

    result["violation_count"] = len(findings)

    if len(findings) > 0:
        result["severity_counts"]["high"] = len(findings)
        result["max_severity"] = "high"
    else:
        result["max_severity"] = None

    # Diagnostics
    if malformed_count:
        result["metadata"]["malformed_lines"] = malformed_count

    return result
