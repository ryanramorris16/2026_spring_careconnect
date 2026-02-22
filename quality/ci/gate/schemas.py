# ==========================================================
# schemas.py
# ----------------------------------------------------------
# Defines the standardized data structures used by the
# Quality Gate normalization and policy evaluation layers.
#
# DO NOT embed policy logic here.
# This file defines STRUCTURE ONLY.
# ==========================================================

from typing import Dict, Any


def base_tool_result(tool_name: str) -> Dict[str, Any]:
    """
    Returns the standardized structure every tool parser must output.

    All parsers MUST return this structure.
    Missing values must use safe defaults.
    """

    return {
        "tool": tool_name,

        # Whether the tool actually executed
        "executed": False,

        # True if the tool crashed, artifact missing, or runtime error occurred
        "runtime_error": False,

        # Raw violation count (total findings)
        "violation_count": 0,

        # Severity breakdown
        "severity_counts": {
            "critical": 0,
            "high": 0,
            "medium": 0,
            "low": 0,
            "info": 0
        },

        # Highest severity encountered
        "max_severity": None,

        # Tool-specific metadata (optional)
        "metadata": {}
    }