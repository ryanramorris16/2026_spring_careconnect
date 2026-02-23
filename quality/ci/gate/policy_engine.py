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
#     overall_block becomes True, and the workflow must fail.
#   - Additionally, runtime errors / missing artifacts / non-execution are
#     treated as violations (governance requires deterministic enforcement).
# ==========================================================

import json
import yaml
from pathlib import Path

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

def load_policy():
    """
    Load policy.yaml and return both:
      - gate config (global controls)
      - tools config (per-tool enforcement rules)

    This keeps policy.yaml as the single governance source of truth.
    """
    with open(POLICY_FILE) as f:
        data = yaml.safe_load(f) or {}

    # Gate config is optional; defaults to enforce mode.
    gate_cfg = data.get("gate", {}) or {}
    tools_cfg = data.get("tools", {}) or {}

    return gate_cfg, tools_cfg

def load_normalized():
    """
    Load the normalized tool results produced by normalize.py.
    Returns a list of tool result dicts (one per tool).
    """
    with open(NORMALIZED_FILE) as f:
        return json.load(f)


def severity_rank(level):
    """
    Convert severity labels into an ordered integer ranking.

    The normalization layer provides max_severity and severity_counts
    using this shared vocabulary.

    Higher rank means more severe.
    """
    order = ["info", "low", "medium", "high", "critical"]
    return order.index(level) if level in order else -1


def evaluate():
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

    normalized_results = load_normalized()

    evaluated = []
    overall_block = False

    # ----------------------------------------------------------
    # Per-tool evaluation
    # ----------------------------------------------------------
    # Each normalized tool result is matched against its policy entry.
    # Tools missing from policy.yaml default to non-blocking and no rules.
    # ----------------------------------------------------------
    for tool_result in normalized_results:
        tool_name = tool_result["tool"]
        tool_policy = tools_policy.get(tool_name, {})

        # Whether this tool is allowed to block merges (on/off switch)
        blocking = tool_policy.get("blocking", False)

        # Tool-specific fail conditions (defined in policy.yaml)
        fail_rules = tool_policy.get("fail_on", {})

        policy_violation = False
        reason = None  # "reason" is set to a stable code used in reporting

        # ----------------------------------------------------------
        # Governance: runtime failures are violations
        # ----------------------------------------------------------
        # Missing artifacts, parser crashes, malformed outputs, etc.
        # MUST block the merge if the tool is blocking.
        # ----------------------------------------------------------
        if tool_result["runtime_error"]:
            policy_violation = True
            reason = "runtime_error"

        # ----------------------------------------------------------
        # Governance: tool not executed is a violation
        # ----------------------------------------------------------
        # Even if runtime_error is not explicitly true, a tool that did not
        # execute is treated as non-compliant. This prevents silent skips.
        # ----------------------------------------------------------
        if not tool_result["executed"]:
            policy_violation = True
            reason = "tool_not_executed"

        # ----------------------------------------------------------
        # Generic count-based rule
        # ----------------------------------------------------------
        # Example (checkstyle):
        #   violation_count: ">0"
        # ----------------------------------------------------------
        if "violation_count" in fail_rules:
            threshold = fail_rules["violation_count"]
            if threshold == ">0" and tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "violation_count_exceeded"

        # ----------------------------------------------------------
        # Flutter-specific error rule
        # ----------------------------------------------------------
        # For Flutter analyze we treat "high" severity count as error_count.
        # (Normalization layer maps analyzer errors -> high)
        # ----------------------------------------------------------
        if "error_count" in fail_rules:
            threshold = fail_rules["error_count"]
            if threshold == ">0":
                if tool_result["severity_counts"]["high"] > 0:
                    policy_violation = True
                    reason = "flutter_errors_present"

        # ----------------------------------------------------------
        # Severity-based rule
        # ----------------------------------------------------------
        # Example:
        #   severity: "high_and_above"
        #   severity: "medium_and_above"
        #   severity: "critical_only"
        # ----------------------------------------------------------
        if "severity" in fail_rules:
            rule = fail_rules["severity"]
            max_sev = tool_result["max_severity"]

            # If a tool found nothing, max_severity may be None.
            if max_sev:
                if rule == "high_and_above" and severity_rank(max_sev) >= severity_rank("high"):
                    policy_violation = True
                    reason = "severity_high_and_above"
                elif rule == "medium_and_above" and severity_rank(max_sev) >= severity_rank("medium"):
                    policy_violation = True
                    reason = "severity_medium_and_above"
                elif rule == "critical_only" and max_sev == "critical":
                    policy_violation = True
                    reason = "severity_critical"

        # ----------------------------------------------------------
        # Dependency-Check rule: any vulnerability triggers failure
        # ----------------------------------------------------------
        if fail_rules.get("any_vulnerability") is True:
            if tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "vulnerability_present"

        # ----------------------------------------------------------
        # Generic "any finding" rule (e.g., TruffleHog)
        # ----------------------------------------------------------
        if fail_rules.get("any_finding") is True:
            if tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "finding_present"

        # ----------------------------------------------------------
        # SonarQube Quality Gate rule
        # ----------------------------------------------------------
        # Normalization maps "ERROR" → violation_count = 1.
        # This keeps policy logic simple.
        # ----------------------------------------------------------
        if "quality_gate" in fail_rules:
            if tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "quality_gate_failed"

        # ----------------------------------------------------------
        # Determine overall merge block decision
        # ----------------------------------------------------------
        if blocking and policy_violation:
            overall_block = True

        # Store evaluated result for reporting and audit artifacts
        evaluated.append({
            "tool": tool_name,
            "blocking": blocking,
            "policy_violation": policy_violation,
            "reason": reason,
            "normalized": tool_result
        })

    # ----------------------------------------------------------
    # Write evaluated.json (single source of truth for reporting)
    # ----------------------------------------------------------
    with open(OUTPUT_FILE, "w") as f:
        json.dump({
            "overall_block": overall_block,
            "results": evaluated
        }, f, indent=2)

    return overall_block


# Allow standalone execution (useful for local debugging)
# NOTE: gate.py is the preferred entrypoint in CI because it also
# runs normalization + report generation.
if __name__ == "__main__":
    blocked = evaluate()
    if blocked:
        print("❌ Policy violation detected. Merge blocked.")
        exit(1)
    else:
        print("✅ All policy checks passed.")
        exit(0)