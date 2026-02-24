# File: quality/ci/gate/policy_engine.py
# ==========================================================
# policy_engine.py
# ----------------------------------------------------------
# Policy Evaluation Layer (Layer 2 of the Quality Gate Engine)
#
# Purpose:
#   Apply governance policy (policy.yaml) to normalized tool
#   results (normalized.json) and decide whether the merge
#   should be blocked.
#
# Inputs:
#   quality/analysis/normalized/normalized.json
#     Produced by normalize.py.
#     Contains standardized per-tool results with findings,
#     severity counts, and execution state.
#
#   quality/ci/gate/policy.yaml
#     Version-controlled governance rules.
#     Defines per-tool blocking flags and fail conditions.
#
# Output:
#   quality/analysis/evaluated/evaluated.json
#   {
#     "overall_block":       bool,
#     "generated_at":        "UTC timestamp",
#     "blocking_results":    [ ... ],   ← tools that can block the merge
#     "non_blocking_results":[ ... ]    ← advisory tools (informational only)
#   }
#
# Enforcement Contract:
#   - A tool marked blocking=true that violates its policy rules
#     sets overall_block=True.
#   - Runtime errors and missing artifacts are violations for
#     blocking tools (fail-safe governance).
#   - Disabled tools (metadata.status=disabled) are never flagged
#     as violations regardless of blocking flag.
#   - gate.mode (enforce vs report_only) is NOT applied here.
#     gate.py is the single authority for exit-code decisions.
#
# Execution:
#   Recommended:  python -m quality.ci.gate.policy_engine
#   Direct:       python quality/ci/gate/policy_engine.py
# ==========================================================

import json
from datetime import datetime, timezone
from pathlib import Path

import yaml

from .utils import is_severity_at_least, SEVERITY_ORDER

# ----------------------------------------------------------
# Input / Output paths
# ----------------------------------------------------------
NORMALIZED_FILE = Path("quality/analysis/normalized/normalized.json")
POLICY_FILE     = Path("quality/ci/gate/policy.yaml")
EVALUATED_DIR   = Path("quality/analysis/evaluated")
OUTPUT_FILE     = EVALUATED_DIR / "evaluated.json"


def load_policy() -> tuple[dict, dict]:
    """
    Load and parse policy.yaml.

    Returns:
        gate_cfg:  Global gate configuration (mode, etc.).
        tools_cfg: Per-tool enforcement rules.

    Note:
        gate.mode is returned here for gate.py to consume.
        This function does not apply gate.mode itself.
    """
    with open(POLICY_FILE, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}

    gate_cfg  = data.get("gate", {}) or {}
    tools_cfg = data.get("tools", {}) or {}

    return gate_cfg, tools_cfg


def load_normalized() -> tuple[list[dict], dict]:
    """
    Load normalized.json produced by normalize.py.

    Returns:
        results:  List of per-tool result dicts.
        summary:  Top-level summary fields (generated_at, max_severity, etc.).
    """
    with open(NORMALIZED_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    # normalize.py writes a top-level wrapper dict — extract results list
    results = data.get("results", [])
    summary = {
        "generated_at":     data.get("generated_at", ""),
        "tool_count":       data.get("tool_count", 0),
        "total_violations": data.get("total_violations", 0),
        "max_severity":     data.get("max_severity"),
    }

    return results, summary


def _is_disabled(tool_result: dict) -> bool:
    """
    Return True if the tool is explicitly marked as disabled.

    Disabled tools have metadata.status=disabled and executed=False.
    They are never treated as violations regardless of policy.

    Args:
        tool_result: A normalized tool result dict.

    Returns:
        True if the tool is disabled, False otherwise.
    """
    metadata = tool_result.get("metadata", {}) or {}
    return (
        not tool_result.get("executed", False)
        and metadata.get("status") == "disabled"
    )


def _evaluate_tool(
    tool_result: dict,
    tool_policy: dict,
) -> tuple[bool, str | None]:
    """
    Evaluate a single tool result against its policy rules.

    Args:
        tool_result: Normalized result dict from normalize.py.
        tool_policy: Policy entry for this tool from policy.yaml.

    Returns:
        policy_violation: True if any rule was violated.
        reason:           Code identifying the first violation found,
                          or None if no violation occurred.

    Violation reason codes:
        runtime_error           → Parser crashed or artifact unreadable.
        tool_not_executed       → Tool did not run (not disabled).
        violation_count_exceeded→ Total findings exceeded threshold.
        flutter_errors_present  → Flutter analyzer errors found.
        severity_high_and_above → Max severity is high or critical.
        severity_medium_and_above→ Max severity is medium or above.
        severity_critical       → Max severity is critical.
        vulnerability_present   → Any CVE found (dependency_check).
        finding_present         → Any finding found (trufflehog).
        quality_gate_failed     → Sonar quality gate status is ERROR.
    """
    policy_violation = False
    reason: str | None = None

    def violate(code: str) -> None:
        """Record first violation reason; subsequent violations are ignored."""
        nonlocal policy_violation, reason
        policy_violation = True
        if reason is None:
            reason = code

    fail_rules      = tool_policy.get("fail_on", {}) or {}
    violation_count = int(tool_result.get("violation_count", 0) or 0)
    severity_counts = tool_result.get("severity_counts", {}) or {}
    max_sev         = tool_result.get("max_severity")

    # ----------------------------------------------------------
    # Governance: runtime error is always a violation
    # ----------------------------------------------------------
    # Covers: parser crashes, malformed artifacts, unreadable files.
    # ----------------------------------------------------------
    if tool_result.get("runtime_error", False):
        violate("runtime_error")

    # ----------------------------------------------------------
    # Governance: tool not executed is a violation
    # ----------------------------------------------------------
    # Prevents silent skips from masking enforcement gaps.
    # Note: disabled tools are excluded before this is called.
    # ----------------------------------------------------------
    if not tool_result.get("executed", False):
        violate("tool_not_executed")

    # ----------------------------------------------------------
    # Rule: violation_count
    # ----------------------------------------------------------
    # Triggers when total findings exceed the configured threshold.
    # Example in policy.yaml:
    #   fail_on:
    #     violation_count: ">0"
    # ----------------------------------------------------------
    if "violation_count" in fail_rules:
        threshold = fail_rules["violation_count"]
        if threshold == ">0" and violation_count > 0:
            violate("violation_count_exceeded")

    # ----------------------------------------------------------
    # Rule: error_count (Flutter-specific)
    # ----------------------------------------------------------
    # Flutter analyzer errors are normalized to "high" severity.
    # This rule treats high-severity Flutter findings as errors.
    # Example in policy.yaml:
    #   fail_on:
    #     error_count: ">0"
    # ----------------------------------------------------------
    if "error_count" in fail_rules:
        threshold = fail_rules["error_count"]
        if threshold == ">0":
            flutter_errors = int(severity_counts.get("high", 0) or 0)
            if flutter_errors > 0:
                violate("flutter_errors_present")

    # ----------------------------------------------------------
    # Rule: severity
    # ----------------------------------------------------------
    # Triggers when max_severity meets or exceeds the threshold.
    # Uses is_severity_at_least() from utils.py for consistent
    # severity comparison across the gate engine.
    #
    # Example in policy.yaml:
    #   fail_on:
    #     severity: "high_and_above"
    # ----------------------------------------------------------
    if "severity" in fail_rules and max_sev:
        rule = fail_rules["severity"]

        if rule == "high_and_above" and is_severity_at_least(max_sev, "high"):
            violate("severity_high_and_above")
        elif rule == "medium_and_above" and is_severity_at_least(max_sev, "medium"):
            violate("severity_medium_and_above")
        elif rule == "critical_only" and max_sev == "critical":
            violate("severity_critical")

    # ----------------------------------------------------------
    # Rule: any_vulnerability (Dependency-Check)
    # ----------------------------------------------------------
    # Blocks on any known CVE regardless of severity level.
    # Example in policy.yaml:
    #   fail_on:
    #     any_vulnerability: true
    # ----------------------------------------------------------
    if fail_rules.get("any_vulnerability") is True and violation_count > 0:
        violate("vulnerability_present")

    # ----------------------------------------------------------
    # Rule: any_finding (TruffleHog / secrets)
    # ----------------------------------------------------------
    # Blocks on any detected secret regardless of verified status.
    # Example in policy.yaml:
    #   fail_on:
    #     any_finding: true
    # ----------------------------------------------------------
    if fail_rules.get("any_finding") is True and violation_count > 0:
        violate("finding_present")

    # ----------------------------------------------------------
    # Rule: quality_gate (Sonar)
    # ----------------------------------------------------------
    # Blocks when Sonar's quality gate status is ERROR.
    # normalize.py maps ERROR → violation_count = 1.
    # Example in policy.yaml:
    #   fail_on:
    #     quality_gate: true
    # ----------------------------------------------------------
    if fail_rules.get("quality_gate") is True and violation_count > 0:
        violate("quality_gate_failed")

    return policy_violation, reason


def evaluate() -> bool:
    """
    Evaluate all normalized tool results against policy.yaml.

    Produces evaluated.json with blocking and non-blocking results
    clearly separated. Blocking results are listed first.

    Returns:
        overall_block: True if any blocking tool violated its policy.

    Side effects:
        Writes quality/analysis/evaluated/evaluated.json.
    """
    # Ensure output directory exists before writing
    EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

    gate_cfg, tools_policy = load_policy()
    normalized_results, normalized_summary = load_normalized()

    blocking_results:     list[dict] = []
    non_blocking_results: list[dict] = []
    overall_block = False

    # ----------------------------------------------------------
    # Evaluate each tool result
    # ----------------------------------------------------------
    for tool_result in normalized_results:
        tool_name   = tool_result.get("tool", "unknown_tool")
        tool_policy = tools_policy.get(tool_name, {}) or {}
        blocking    = bool(tool_policy.get("blocking", False))

        # ----------------------------------------------------------
        # Skip disabled tools entirely
        # ----------------------------------------------------------
        # Disabled tools (e.g. Sonar before account setup) must not
        # be flagged as violations. They are recorded as non-blocking
        # advisory entries for transparency.
        # ----------------------------------------------------------
        if _is_disabled(tool_result):
            non_blocking_results.append({
                "tool":             tool_name,
                "blocking":         False,
                "policy_violation": False,
                "reason":           "disabled",
                "normalized":       tool_result,
            })
            continue

        # Evaluate the tool against its policy rules
        policy_violation, reason = _evaluate_tool(tool_result, tool_policy)

        # A blocking tool with a violation sets the overall merge block
        if blocking and policy_violation:
            overall_block = True

        # Build the evaluated record for this tool
        evaluated_record = {
            "tool":             tool_name,
            "blocking":         blocking,
            "policy_violation": policy_violation,
            "reason":           reason,
            "normalized":       tool_result,
        }

        # Route to the correct section based on blocking flag
        if blocking:
            blocking_results.append(evaluated_record)
        else:
            non_blocking_results.append(evaluated_record)

    # ----------------------------------------------------------
    # Compose evaluated.json
    # ----------------------------------------------------------
    # Blocking results are listed first for immediate visibility.
    # Non-blocking results follow as advisory information.
    # ----------------------------------------------------------
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    evaluated_doc = {
        "overall_block":        overall_block,
        "generated_at":         generated_at,
        "blocking_results":     blocking_results,
        "non_blocking_results": non_blocking_results,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(evaluated_doc, f, indent=2)

    # ----------------------------------------------------------
    # Console summary
    # ----------------------------------------------------------
    blocking_violations = sum(
        1 for r in blocking_results if r["policy_violation"]
    )
    print(f"[policy_engine] Blocking tools evaluated : {len(blocking_results)}")
    print(f"[policy_engine] Blocking violations      : {blocking_violations}")
    print(f"[policy_engine] Non-blocking tools       : {len(non_blocking_results)}")
    print(f"[policy_engine] Overall block            : {overall_block}")
    print(f"[policy_engine] Output written to        : {OUTPUT_FILE}")

    return overall_block


if __name__ == "__main__":
    blocked = evaluate()
    if blocked:
        print("❌ Policy violation detected. Merge blocked.")
        raise SystemExit(1)
    print("✅ All policy checks passed.")
    raise SystemExit(0)
