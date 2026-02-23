# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/schemas.py
# ==========================================================
# schemas.py
# ----------------------------------------------------------
# Standardized Data Structures for the Quality Gate Engine
#
# This module defines the canonical structure used across:
#   - normalize.py       (Layer 1)
#   - policy_engine.py   (Layer 2)
#   - gate.py            (Orchestration + reporting)
#   - humanize.py        (Human-readable findings)
#
# DESIGN PRINCIPLE:
#   This file defines STRUCTURE ONLY.
#   It must NEVER contain:
#     - Policy thresholds
#     - Enforcement logic
#     - Tool-specific parsing rules
#
# All parsers MUST return data in this format so the policy
# layer can remain tool-agnostic and deterministic.
#
# IMPORTANT:
# - Prefer stable, additive changes to this schema.
# - Do not remove fields once adopted (backward compatibility).
#
# SECURITY NOTE (secrets tools like TruffleHog):
# - Parsers MUST NOT store raw secret/token values in findings or metadata.
# - Human-readable artifacts are uploaded and may be visible to others.
# ==========================================================

from __future__ import annotations

from typing import Any, Dict, List, Optional


def base_tool_result(tool_name: str) -> Dict[str, Any]:
    """
    Factory function returning the standardized result structure
    that every tool parser MUST return.

    Contract for parsers:
      - Always return this structure.
      - Do not remove fields.
      - Populate fields as accurately as possible.
      - Use safe defaults for missing data.

    Why this matters:
      - Policy evaluation relies on consistent keys.
      - Reporting depends on stable structure.
      - Future tools can be added without changing policy logic.
    """

    return {
        # ------------------------------------------------------
        # Tool identifier
        # ------------------------------------------------------
        # Must match:
        #   - policy.yaml key
        #   - normalize.py registration name
        #   - reporting output
        # ------------------------------------------------------
        "tool": tool_name,

        # ------------------------------------------------------
        # Execution + governance status
        # ------------------------------------------------------
        # artifact_present:
        #   True if the expected raw artifact exists on disk.
        #
        # executed:
        #   True if the tool ran AND the parser successfully read the artifact.
        #
        # runtime_error:
        #   True if:
        #     - Parser crashed
        #     - Artifact malformed/unreadable
        #     - Tool output missing required fields
        #
        # Governance rule (fail-safe):
        #   Missing artifacts or runtime errors are treated as violations
        #   when the tool is configured as blocking in policy.yaml.
        # ------------------------------------------------------
        "artifact_present": False,
        "executed": False,
        "runtime_error": False,

        # ------------------------------------------------------
        # Findings (normalized)
        # ------------------------------------------------------
        # findings:
        #   A list of normalized finding objects.
        #
        # Recommended keys per finding:
        #   - severity: critical|high|medium|low|info
        #   - message: human-readable description (NO secrets)
        #   - rule: tool rule/check id (optional)
        #   - file: repo-relative path (optional)
        #   - line: integer line number (optional)
        #   - rule_url: reference link (optional)
        #
        # Parsers may include additional keys, but the above are the
        # common set used by humanize.py and reporting.
        # ------------------------------------------------------
        "findings": [],  # type: List[Dict[str, Any]]

        # ------------------------------------------------------
        # Finding counts
        # ------------------------------------------------------
        # violation_count:
        #   Total number of findings detected.
        #
        # This value is used by:
        #   - violation_count rules
        #   - any_finding rules
        #   - any_vulnerability rules
        # ------------------------------------------------------
        "violation_count": 0,

        # ------------------------------------------------------
        # Severity breakdown
        # ------------------------------------------------------
        # Standardized severity vocabulary:
        #   critical > high > medium > low > info
        #
        # All parsers must map tool-specific severity scales
        # into this normalized set.
        # ------------------------------------------------------
        "severity_counts": {
            "critical": 0,
            "high": 0,
            "medium": 0,
            "low": 0,
            "info": 0,
        },

        # ------------------------------------------------------
        # Maximum severity encountered
        # ------------------------------------------------------
        # max_severity:
        #   - One of: critical, high, medium, low, info
        #   - Or None if no findings were detected
        # ------------------------------------------------------
        "max_severity": None,  # type: Optional[str]

        # ------------------------------------------------------
        # Tool-specific metadata
        # ------------------------------------------------------
        # Optional dictionary for:
        #   - error messages
        #   - parser diagnostics
        #   - additional structured context
        #
        # SECURITY: Do not store raw secrets here.
        # ------------------------------------------------------
        "metadata": {},  # type: Dict[str, Any]
    }
