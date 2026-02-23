# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/parsers/trufflehog.py
# ==========================================================
# trufflehog.py
# ----------------------------------------------------------
# TruffleHog Parser (Secrets Scanning)
#
# Purpose:
#   Parse TruffleHog output and normalize results into the
#   standard schema defined in schemas.py.
#
# Expected raw artifact:
#   quality/analysis/raw/trufflehog.jsonl
#
# Output format:
#   TruffleHog commonly emits JSONL (one JSON object per line).
#   Depending on flags/version, it may also emit non-finding lines.
#
# Normalization strategy (production-safe, best-effort):
#   - Iterate each line, attempt JSON parse.
#   - Count objects that look like findings.
#   - Treat any findings as high severity by default.
#
# Policy linkage:
#   - policy.yaml currently enforces:
#       any_finding: true
#     meaning ANY secret finding blocks the merge (if blocking=true).
#
# Notes:
#   - This parser is intentionally conservative:
#       Unknown/malformed lines are ignored (but file existence is required).
#   - If you later want richer reporting (detector name, file path, line number),
#     capture a limited sample list into result["metadata"] (e.g., top 10).
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_trufflehog(raw_dir: Path):
    """
    Parse TruffleHog JSONL output and return a standardized result dictionary.

    Contract:
      - Always return base_tool_result structure.
      - Missing artifact = runtime_error.
      - Parsing exceptions = runtime_error with diagnostic metadata.

    Artifact expectation:
      - Each line may be a JSON object.
      - Findings are identified via presence of common keys.
    """
    tool_name = "trufflehog"

    # Initialize standardized result structure
    result = base_tool_result(tool_name)

    # Expected artifact path produced by CI workflow step
    artifact = raw_dir / "trufflehog.jsonl"

    # ------------------------------------------------------
    # Artifact existence check
    # ------------------------------------------------------
    # Missing report indicates:
    #   - TruffleHog did not run
    #   - workflow output path mismatch
    #   - tool failed before writing output
    # Treated as runtime_error (governance violation).
    # ------------------------------------------------------
    if not artifact.exists():
        result["runtime_error"] = True
        return result

    # Mark tool as executed (artifact exists; parsing will be attempted)
    result["executed"] = True

    findings = 0

    try:
        # --------------------------------------------------
        # TruffleHog output is JSONL; parse line-by-line
        # --------------------------------------------------
        with open(artifact, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue

                # Some lines may be summary or non-JSON; ignore safely.
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError:
                    continue

                # --------------------------------------------------
                # Best-effort finding detection
                # --------------------------------------------------
                # TruffleHog findings typically include fields such as:
                #   - DetectorName
                #   - SourceMetadata
                #   - Verified
                #   - Raw / Redacted
                #
                # We count objects that contain DetectorName or SourceMetadata
                # as findings. This is resilient across minor schema changes.
                # --------------------------------------------------
                if "DetectorName" in obj or "SourceMetadata" in obj:
                    findings += 1

        # Total findings count (used by "any_finding" policy rules)
        result["violation_count"] = findings

        # ------------------------------------------------------
        # Severity mapping
        # ------------------------------------------------------
        # Secrets are treated as high severity by default.
        # You can change policy behavior via policy.yaml, but
        # severity classification here is intentionally strict.
        # ------------------------------------------------------
        if findings > 0:
            result["severity_counts"]["high"] = findings
            result["max_severity"] = "high"
        else:
            result["max_severity"] = None

    except Exception as e:
        # Any exception reading or parsing the artifact is treated as runtime_error.
        result["runtime_error"] = True
        result["metadata"]["error"] = str(e)

    return result