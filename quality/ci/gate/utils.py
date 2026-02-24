# File: quality/ci/gate/utils.py
# ==========================================================
# utils.py
# ----------------------------------------------------------
# Shared Utility Functions for the Quality Gate Engine
#
# Purpose:
#   Provides common helper functions used across the gate engine,
#   including parsers, normalizer, policy engine, and reporting.
#
# Design Principle:
#   This file contains LOGIC ONLY — no schema definitions, no policy
#   thresholds, and no tool-specific parsing rules.
#
#   - Schema structure belongs in schemas.py
#   - Policy thresholds belong in policy.yaml
#   - Tool-specific logic belongs in parsers/
#
# Consumers:
#   - quality/ci/gate/parsers/*.py  (severity resolution)
#   - quality/ci/gate/normalize.py  (severity resolution)
#   - quality/ci/gate/policy_engine.py (severity comparison)
# ==========================================================

from __future__ import annotations


# ----------------------------------------------------------
# Normalized severity vocabulary (in priority order)
# ----------------------------------------------------------
# This is the canonical severity order used across the entire
# gate engine. All tools map their native severities into this
# vocabulary via their individual SEVERITY_MAP definitions.
#
# Order: critical (most severe) → info (least severe)
# ----------------------------------------------------------
SEVERITY_ORDER = ["critical", "high", "medium", "low", "info"]


def determine_max_severity(severity_counts: dict) -> str | None:
    """
    Return the highest normalized severity level that has at least
    one finding, using the canonical priority order:
        critical → high → medium → low → info

    This function is the single source of truth for max_severity
    resolution across all parsers and the normalizer. It must not
    be duplicated in individual parser files.

    Args:
        severity_counts: Dict mapping severity label to integer count.
                         Expected keys: critical, high, medium, low, info.
                         Missing keys are treated as zero (safe default).

    Returns:
        The highest severity string with a non-zero count, or None
        if no findings were recorded across any severity level.

    Examples:
        >>> determine_max_severity({"critical": 0, "high": 3, "medium": 1, "low": 0, "info": 0})
        'high'
        >>> determine_max_severity({"critical": 0, "high": 0, "medium": 0, "low": 0, "info": 0})
        None
    """
    for level in SEVERITY_ORDER:
        if severity_counts.get(level, 0) > 0:
            return level

    # No findings recorded across any severity level
    return None


def is_severity_at_least(severity: str | None, threshold: str) -> bool:
    """
    Return True if the given severity is equal to or more severe than
    the threshold, using the canonical priority order.

    Useful for policy enforcement rules such as "medium_and_above".

    Args:
        severity:  A normalized severity string, or None.
        threshold: A normalized severity string to compare against.

    Returns:
        True if severity is >= threshold in priority order, else False.
        Returns False if severity is None.

    Examples:
        >>> is_severity_at_least("high", "medium")
        True
        >>> is_severity_at_least("low", "high")
        False
        >>> is_severity_at_least(None, "low")
        False
    """
    if severity is None:
        return False

    # Lower index = more severe in SEVERITY_ORDER
    try:
        severity_index  = SEVERITY_ORDER.index(severity)
        threshold_index = SEVERITY_ORDER.index(threshold)
    except ValueError:
        # Unrecognized severity label — treat as not meeting threshold
        return False

    return severity_index <= threshold_index
