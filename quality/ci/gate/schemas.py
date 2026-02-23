# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/schemas.py
# ==========================================================
# schemas.py
# ----------------------------------------------------------
# Standardized Data Structures for the Quality Gate Engine
#
# This module defines the canonical structure used across:
#   - normalize.py   (Layer 1)
#   - policy_engine.py (Layer 2)
#   - gate.py (reporting layer)
#
# DESIGN PRINCIPLE:
#   This file defines STRUCTURE ONLY.
#   It must NEVER contain:
#     - Policy thresholds
#     - Enforcement logic
#     - Tool-specific parsing rules
#
# All parsers MUST return data in this format so the policy
# layer can remain tool-agnostic.
# ==========================================================

from typing import Dict, Any


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
        # Execution status
        # ------------------------------------------------------
        # executed:
        #   True if the tool ran and produced a readable artifact.
        #
        # runtime_error:
        #   True if:
        #     - Artifact missing
        #     - Parser crashed
        #     - Tool failed unexpectedly
        #
        # Governance rule:
        #   runtime_error or executed=False will trigger policy
        #   violations in blocking tools.
        # ------------------------------------------------------
        "executed": False,
        "runtime_error": False,

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
        #
        # This allows policy_engine.py to apply consistent
        # severity-based rules regardless of tool.
        # ------------------------------------------------------
        "severity_counts": {
            "critical": 0,
            "high": 0,
            "medium": 0,
            "low": 0,
            "info": 0
        },

        # ------------------------------------------------------
        # Maximum severity encountered
        # ------------------------------------------------------
        # max_severity:
        #   - One of: critical, high, medium, low, info
        #   - Or None if no findings were detected
        #
        # Used by severity-based policy rules:
        #   - high_and_above
        #   - medium_and_above
        #   - critical_only
        # ------------------------------------------------------
        "max_severity": None,

        # ------------------------------------------------------
        # Tool-specific metadata
        # ------------------------------------------------------
        # Optional dictionary for:
        #   - error messages
        #   - parser diagnostics
        #   - additional structured context
        #
        # Policy layer should not rely on metadata fields
        # unless explicitly designed to.
        # ------------------------------------------------------
        "metadata": {}
    }