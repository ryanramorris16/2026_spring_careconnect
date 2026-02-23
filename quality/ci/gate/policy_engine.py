# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/policy_engine.py
# ==========================================================
# policy_engine.py
# ----------------------------------------------------------
# Policy Evaluation Layer (Layer 2)
#
# This module is Layer 2 of the Quality Gate subsystem.
#
# Purpose:
#   Apply governance policy (policy.yaml) to normalized tool
#   results (normalized.json) and decide whether the merge
#   should be blocked.
#
# Inputs:
#   - quality/analysis/normalized/normalized.json
#       Produced by normalize.py
#       Contains standardized per-tool results (counts + severities)
#
#   - quality/ci/gate/policy.yaml
#       Version-controlled governance policy (blocking flags + thresholds)
#
# Outputs:
#   - quality/analysis/evaluated/evaluated.json
#       Contains:
#         overall_block: bool
#         results: list of per-tool evaluated states
#
# Enforcement Contract:
#   - If ANY tool that is marked "blocking: true" violates its policy,
#     overall_block becomes True.
#   - Additionally, runtime errors / missing artifacts / non-execution are
#     treated as violations for blocking tools (fail-safe governance).
#
# NOTE ON gate.mode:
#   - policy.yaml may define gate.mode (enforce vs report_only).
#   - This file evaluates violations consistently either way.
#   - The final exit-code decision is enforced by gate.py (single authority).
# ==========================================================

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Tuple

import yaml

# ----------------------------------------------------------
# Input / Output locations
# ----------------------------------------------------------
NORMALIZED_FILE = Path("quality/analysis/normalized/normalized.json")
POLICY_FILE = Path("quality/ci/gate/policy.yaml")

# The evaluated output folder is created automatically so workflows can
# upload the entire quality/analysis directory as an artifact bundle.
EVALUATED_DIR = Path("quality/analysis/evaluated")
EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

OUTPUT_FILE = EVALUATED_DIR / "evaluated.json"

# Shared normalized severity vocabulary (lowest → highest)
SEVERITY_ORDER: List[str] = ["info", "low", "medium", "high", "critical"]


def load_policy() -> Tuple[Dict[str, Any], Dict[str, Any]]:
    """
    Load policy.yaml and return both:
      - gate config (global controls)
      - tools config (per-tool enforcement rules)

    This keeps policy.yaml as the single governance source of truth.
    """
    with open(POLICY_FILE, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}

    # Gate config is optional; defaults to enforce mode.
    gate_cfg = data.get("gate", {}) or {}
    tools_cfg = data.get("tools", {}) or {}

    return gate_cfg, tools_cfg


def load_normalized() -> List[Dict[str, Any]]:
    """
    Load the normalized tool results produced by normalize.py.
    Returns a list of tool result dicts (one per tool).
    """
    with open(NORMALIZED_FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def severity_rank(level: str | None) -> int:
    """
    Convert severity labels into an ordered integer ranking.

    The normalization layer provides max_severity and severity_counts
    using this shared vocabulary.

    Higher rank means more severe.
    """
    if not level:
        return -1
    try:
        return SEVERITY_ORDER.index(level)
    except ValueError:
        return -1


def evaluate() -> bool:
    """
    Evaluate normalized results against policy.yaml.

    Returns:
      overall_block (bool)

    Side effects:
      Writes quality/analysis/evaluated/evaluated.json
    """
    # gate_cfg is intentionally loaded here to validate policy.yaml structure,
    # but gate.mode enforcement is handled in gate.py (single exit-code authority).
    gate_cfg, tools_policy = load_policy()
    _ = gate_cfg  # keep linting quiet; gate.py owns gate.mode enforcement

    normalized_results = load_normalized()

    evaluated: List[Dict[str, Any]] = []
    overall_block = False

    # ----------------------------------------------------------
    # Per-tool evaluation
    # ----------------------------------------------------------
    # Each normalized tool result is matched against its policy entry.
    # Tools missing from policy.yaml default to non-blocking and no rules.
    # ----------------------------------------------------------
    for tool_result in normalized_results:
        tool_name = tool_result.get("tool", "unknown_tool")
        tool_policy = tools_policy.get(tool_name, {}) or {}

        # Whether this tool is allowed to block merges (on/off switch)
        blocking = bool(tool_policy.get("blocking", False))

        # Tool-specific fail conditions (defined in policy.yaml)
        fail_rules: Dict[str, Any] = tool_policy.get("fail_on", {}) or {}

        policy_violation = False
        reason: str | None = None  # stable code used in reporting

        def violate(code: str) -> None:
            """Set violation + first reason (do not overwrite earlier root cause)."""
            nonlocal policy_violation, reason
            policy_violation = True
            if reason is None:
                reason = code

        # ----------------------------------------------------------
        # Governance: runtime failures are violations
        # ----------------------------------------------------------
        # Missing artifacts, parser crashes, malformed outputs, etc.
        # MUST block the merge if the tool is blocking.
        # ----------------------------------------------------------
        if bool(tool_result.get("runtime_error", False)):
            violate("runtime_error")

        # ----------------------------------------------------------
        # Governance: tool not executed is a violation
        # ----------------------------------------------------------
        # Even if runtime_error is not explicitly true, a tool that did not
        # execute is treated as non-compliant. This prevents silent skips.
        # ----------------------------------------------------------
        if not bool(tool_result.get("executed", False)):
            violate("tool_not_executed")

        # Convenience locals from normalized schema (with safe defaults)
        violation_count = int(tool_result.get("violation_count", 0) or 0)
        severity_counts = tool_result.get("severity_counts", {}) or {}
        max_sev = tool_result.get("max_severity")

        # ----------------------------------------------------------
        # Generic count-based rule
        # Example:
        #   violation_count: ">0"
        # ----------------------------------------------------------
        if "violation_count" in fail_rules:
            threshold = fail_rules.get("violation_count")
            if threshold == ">0" and violation_count > 0:
                violate("violation_count_exceeded")

        # ----------------------------------------------------------
        # Flutter-specific error rule
        # For Flutter analyze we treat "high" severity count as error_count.
        # (Normalization layer maps analyzer errors -> high)
        # Example:
        #   error_count: ">0"
        # ----------------------------------------------------------
        if "error_count" in fail_rules:
            threshold = fail_rules.get("error_count")
            if threshold == ">0":
                flutter_errors = int(severity_counts.get("high", 0) or 0)
                if flutter_errors > 0:
                    violate("flutter_errors_present")

        # ----------------------------------------------------------
        # Severity-based rule
        # Example:
        #   severity: "high_and_above"
        #   severity: "medium_and_above"
        #   severity: "critical_only"
        # ----------------------------------------------------------
        if "severity" in fail_rules and max_sev:
            rule = fail_rules.get("severity")

            if rule == "high_and_above" and severity_rank(max_sev) >= severity_rank("high"):
                violate("severity_high_and_above")
            elif rule == "medium_and_above" and severity_rank(max_sev) >= severity_rank("medium"):
                violate("severity_medium_and_above")
            elif rule == "critical_only" and max_sev == "critical":
                violate("severity_critical")

        # ----------------------------------------------------------
        # Dependency-Check rule: any vulnerability triggers failure
        # Example:
        #   any_vulnerability: true
        # ----------------------------------------------------------
        if fail_rules.get("any_vulnerability") is True and violation_count > 0:
            violate("vulnerability_present")

        # ----------------------------------------------------------
        # Generic "any finding" rule (e.g., TruffleHog)
        # Example:
        #   any_finding: true
        # ----------------------------------------------------------
        if fail_rules.get("any_finding") is True and violation_count > 0:
            violate("finding_present")

        # ----------------------------------------------------------
        # Sonar Quality Gate rule (SonarQube or SonarCloud)
        # Normalization maps "ERROR"/"FAILED" → violation_count = 1.
        # Example:
        #   quality_gate: true
        # ----------------------------------------------------------
        if fail_rules.get("quality_gate") is True and violation_count > 0:
            violate("quality_gate_failed")

        # ----------------------------------------------------------
        # Determine overall merge block decision
        # ----------------------------------------------------------
        if blocking and policy_violation:
            overall_block = True

        # Store evaluated result for reporting and audit artifacts
        evaluated.append(
            {
                "tool": tool_name,
                "blocking": blocking,
                "policy_violation": policy_violation,
                "reason": reason,
                "normalized": tool_result,
            }
        )

    # ----------------------------------------------------------
    # Write evaluated.json (single source of truth for reporting)
    # ----------------------------------------------------------
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "overall_block": overall_block,
                "results": evaluated,
            },
            f,
            indent=2,
        )

    return overall_block


# Allow standalone execution (useful for local debugging)
# NOTE: gate.py is the preferred entrypoint in CI because it also
# runs normalization + report generation and applies gate.mode.
if __name__ == "__main__":
    blocked = evaluate()
    if blocked:
        print("❌ Policy violation detected. Merge blocked.")
        raise SystemExit(1)
    print("✅ All policy checks passed.")
    raise SystemExit(0)
