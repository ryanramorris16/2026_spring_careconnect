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
#   quality/ci/gate/policy.yaml
#
# Output:
#   quality/analysis/evaluated/evaluated.json
#   {
#     "overall_block":       bool,
#     "generated_at":        "UTC timestamp",
#     "blocking_results":    [ ... ],
#     "non_blocking_results":[ ... ]
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

from .utils import is_severity_at_least

# ----------------------------------------------------------
# Input / Output paths
# ----------------------------------------------------------
NORMALIZED_FILE = Path("quality/analysis/normalized/normalized.json")
POLICY_FILE     = Path("quality/ci/gate/policy.yaml")
EVALUATED_DIR   = Path("quality/analysis/evaluated")
OUTPUT_FILE     = EVALUATED_DIR / "evaluated.json"


# ==========================================================
# Policy + Normalized Loaders
# ==========================================================

def load_policy() -> tuple[dict, dict]:
    """
    Load and parse policy.yaml.

    Returns:
        gate_cfg:  Global gate configuration (mode, etc.).
        tools_cfg: Per-tool enforcement rules.
    """
    with open(POLICY_FILE, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f) or {}
    return data.get("gate", {}) or {}, data.get("tools", {}) or {}


def load_normalized() -> tuple[list[dict], dict]:
    """
    Load normalized.json produced by normalize.py.

    Returns:
        results:  List of per-tool result dicts.
        summary:  Top-level summary fields.
    """
    with open(NORMALIZED_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    results = data.get("results", [])
    summary = {
        "generated_at":     data.get("generated_at", ""),
        "tool_count":       data.get("tool_count", 0),
        "total_violations": data.get("total_violations", 0),
        "max_severity":     data.get("max_severity"),
    }
    return results, summary


# ==========================================================
# Disabled Tool Check
# ==========================================================

def _is_disabled(tool_result: dict) -> bool:
    """
    Return True if the tool is explicitly marked as disabled.

    Disabled tools have metadata.status=disabled and executed=False.
    They are never treated as violations regardless of policy.
    """
    metadata = tool_result.get("metadata", {}) or {}
    return (
        not tool_result.get("executed", False)
        and metadata.get("status") == "disabled"
    )


# ==========================================================
# Rule Evaluators
# ==========================================================

def _check_runtime_error(tool_result: dict) -> str | None:
    return "runtime_error" if tool_result.get("runtime_error", False) else None


def _check_not_executed(tool_result: dict) -> str | None:
    return "tool_not_executed" if not tool_result.get("executed", False) else None


def _check_violation_count(fail_rules: dict, violation_count: int) -> str | None:
    if fail_rules.get("violation_count") == ">0" and violation_count > 0:
        return "violation_count_exceeded"
    return None


def _check_error_count(fail_rules: dict, severity_counts: dict) -> str | None:
    if fail_rules.get("error_count") == ">0":
        if int(severity_counts.get("high", 0) or 0) > 0:
            return "flutter_errors_present"
    return None


def _check_severity(fail_rules: dict, max_sev: str | None) -> str | None:
    if "severity" not in fail_rules or not max_sev:
        return None
    rule = fail_rules["severity"]
    if rule == "high_and_above" and is_severity_at_least(max_sev, "high"):
        return "severity_high_and_above"
    if rule == "medium_and_above" and is_severity_at_least(max_sev, "medium"):
        return "severity_medium_and_above"
    if rule == "critical_only" and max_sev == "critical":
        return "severity_critical"
    return None


def _check_any_vulnerability(fail_rules: dict, violation_count: int) -> str | None:
    if fail_rules.get("any_vulnerability") is True and violation_count > 0:
        return "vulnerability_present"
    return None


def _check_any_finding(fail_rules: dict, violation_count: int) -> str | None:
    if fail_rules.get("any_finding") is True and violation_count > 0:
        return "finding_present"
    return None


def _check_quality_gate(fail_rules: dict, violation_count: int) -> str | None:
    if fail_rules.get("quality_gate") is True and violation_count > 0:
        return "quality_gate_failed"
    return None


def _first_violation(reasons: list[str | None]) -> str | None:
    return next((r for r in reasons if r is not None), None)


# ==========================================================
# Tool Evaluator
# ==========================================================

def _evaluate_tool(
    tool_result: dict,
    tool_policy: dict,
) -> tuple[bool, str | None]:
    """
    Evaluate a single tool result against its policy rules.

    Returns:
        policy_violation: True if any rule was violated.
        reason:           Code identifying the first violation found,
                          or None if no violation occurred.

    Violation reason codes:
        runtime_error            → Parser crashed or artifact unreadable.
        tool_not_executed        → Tool did not run (not disabled).
        violation_count_exceeded → Total findings exceeded threshold.
        flutter_errors_present   → Flutter analyzer errors found.
        severity_high_and_above  → Max severity is high or critical.
        severity_medium_and_above→ Max severity is medium or above.
        severity_critical        → Max severity is critical.
        vulnerability_present    → Any CVE found (dependency_check).
        finding_present          → Any finding found (trufflehog).
        quality_gate_failed      → Sonar quality gate status is ERROR.
    """
    fail_rules      = tool_policy.get("fail_on", {}) or {}
    violation_count = int(tool_result.get("violation_count", 0) or 0)
    severity_counts = tool_result.get("severity_counts", {}) or {}
    max_sev         = tool_result.get("max_severity")

    reason = _first_violation([
        _check_runtime_error(tool_result),
        _check_not_executed(tool_result),
        _check_violation_count(fail_rules, violation_count),
        _check_error_count(fail_rules, severity_counts),
        _check_severity(fail_rules, max_sev),
        _check_any_vulnerability(fail_rules, violation_count),
        _check_any_finding(fail_rules, violation_count),
        _check_quality_gate(fail_rules, violation_count),
    ])

    return reason is not None, reason


# ==========================================================
# Evaluation Helpers
# ==========================================================

def _build_disabled_record(tool_name: str, tool_result: dict) -> dict:
    return {
        "tool":             tool_name,
        "blocking":         False,
        "policy_violation": False,
        "reason":           "disabled",
        "normalized":       tool_result,
    }


def _build_evaluated_record(
    tool_name: str,
    tool_result: dict,
    blocking: bool,
    policy_violation: bool,
    reason: str | None,
) -> dict:
    return {
        "tool":             tool_name,
        "blocking":         blocking,
        "policy_violation": policy_violation,
        "reason":           reason,
        "normalized":       tool_result,
    }


def _print_summary(
    blocking_results: list[dict],
    non_blocking_results: list[dict],
    overall_block: bool,
) -> None:
    blocking_violations = sum(
        1 for r in blocking_results if r["policy_violation"]
    )
    print(f"[policy_engine] Blocking tools evaluated : {len(blocking_results)}")
    print(f"[policy_engine] Blocking violations      : {blocking_violations}")
    print(f"[policy_engine] Non-blocking tools       : {len(non_blocking_results)}")
    print(f"[policy_engine] Overall block            : {overall_block}")
    print(f"[policy_engine] Output written to        : {OUTPUT_FILE}")


# ==========================================================
# Main Evaluator
# ==========================================================

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
    EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

    _, tools_policy     = load_policy()
    normalized_results, _      = load_normalized()

    blocking_results:     list[dict] = []
    non_blocking_results: list[dict] = []
    overall_block = False

    for tool_result in normalized_results:
        tool_name   = tool_result.get("tool", "unknown_tool")
        tool_policy = tools_policy.get(tool_name, {}) or {}
        blocking    = bool(tool_policy.get("blocking", False))

        if _is_disabled(tool_result):
            non_blocking_results.append(
                _build_disabled_record(tool_name, tool_result)
            )
            continue

        policy_violation, reason = _evaluate_tool(tool_result, tool_policy)

        if blocking and policy_violation:
            overall_block = True

        record = _build_evaluated_record(
            tool_name, tool_result, blocking, policy_violation, reason
        )

        if blocking:
            blocking_results.append(record)
        else:
            non_blocking_results.append(record)

    evaluated_doc = {
        "overall_block":        overall_block,
        "generated_at":         datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "blocking_results":     blocking_results,
        "non_blocking_results": non_blocking_results,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(evaluated_doc, f, indent=2)

    _print_summary(blocking_results, non_blocking_results, overall_block)
    return overall_block


if __name__ == "__main__":
    blocked = evaluate()
    if blocked:
        print("❌ Policy violation detected. Merge blocked.")
        raise SystemExit(1)
    print("✅ All policy checks passed.")
    raise SystemExit(0)
