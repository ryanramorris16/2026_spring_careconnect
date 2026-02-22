# ==========================================================
# policy_engine.py
# ----------------------------------------------------------
# Policy Evaluation Layer
#
# Reads:
#   quality/analysis/normalized/normalized.json
#   quality/ci/gate/policy.yaml
#
# Produces:
#   quality/analysis/evaluated/evaluated.json
#
# Applies enforcement rules and determines blocking status.
# ==========================================================

import json
import yaml
from pathlib import Path


NORMALIZED_FILE = Path("quality/analysis/normalized/normalized.json")
POLICY_FILE = Path("quality/ci/gate/policy.yaml")
EVALUATED_DIR = Path("quality/analysis/evaluated")
EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

OUTPUT_FILE = EVALUATED_DIR / "evaluated.json"


def load_policy():
    with open(POLICY_FILE) as f:
        return yaml.safe_load(f)["tools"]


def load_normalized():
    with open(NORMALIZED_FILE) as f:
        return json.load(f)


def severity_rank(level):
    order = ["info", "low", "medium", "high", "critical"]
    return order.index(level) if level in order else -1


def evaluate():
    policy = load_policy()
    normalized_results = load_normalized()

    evaluated = []
    overall_block = False

    for tool_result in normalized_results:
        tool_name = tool_result["tool"]
        tool_policy = policy.get(tool_name, {})

        blocking = tool_policy.get("blocking", False)
        fail_rules = tool_policy.get("fail_on", {})

        policy_violation = False
        reason = None

        # Runtime errors automatically violate
        if tool_result["runtime_error"]:
            policy_violation = True
            reason = "runtime_error"

        # Tool did not execute
        if not tool_result["executed"]:
            policy_violation = True
            reason = "tool_not_executed"

        # Count-based rule
        if "violation_count" in fail_rules:
            threshold = fail_rules["violation_count"]
            if threshold == ">0" and tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "violation_count_exceeded"

        # Flutter error_count rule
        if "error_count" in fail_rules:
            threshold = fail_rules["error_count"]
            if threshold == ">0":
                if tool_result["severity_counts"]["high"] > 0:
                    policy_violation = True
                    reason = "flutter_errors_present"

        # Severity-based rule
        if "severity" in fail_rules:
            rule = fail_rules["severity"]

            max_sev = tool_result["max_severity"]
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

        # Any vulnerability rule
        if fail_rules.get("any_vulnerability") is True:
            if tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "vulnerability_present"

        # Sonar quality gate rule
        if "quality_gate" in fail_rules:
            if tool_result["violation_count"] > 0:
                policy_violation = True
                reason = "quality_gate_failed"

        if blocking and policy_violation:
            overall_block = True

        evaluated.append({
            "tool": tool_name,
            "blocking": blocking,
            "policy_violation": policy_violation,
            "reason": reason,
            "normalized": tool_result
        })

    with open(OUTPUT_FILE, "w") as f:
        json.dump({
            "overall_block": overall_block,
            "results": evaluated
        }, f, indent=2)

    return overall_block


if __name__ == "__main__":
    blocked = evaluate()
    if blocked:
        print("❌ Policy violation detected. Merge blocked.")
        exit(1)
    else:
        print("✅ All policy checks passed.")
        exit(0)