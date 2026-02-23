# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/gate.py
# ==========================================================
# gate.py
# ----------------------------------------------------------
# Quality Gate Orchestrator
#
# This is the single entrypoint for the enforcement engine.
# It coordinates the two-layer design:
#
#   Layer 1 (Normalization): normalize.py
#     - Reads raw tool artifacts from quality/analysis/raw/
#     - Produces normalized results in quality/analysis/normalized/
#
#   Layer 2 (Policy Evaluation): policy_engine.py
#     - Reads normalized.json + policy.yaml
#     - Produces evaluated.json
#     - Determines overall_block (merge approve vs block)
#
# This file does NOT contain:
# - Tool-specific parsing (that belongs in parsers/)
# - Policy thresholds (that belongs in policy.yaml)
#
# Responsibilities of gate.py:
#  1) Invoke normalization
#  2) Invoke policy evaluation
#  3) Generate human + machine readable outputs
#  4) Exit with correct status code (controls merge gating)
#
# Exit Codes (CI enforcement contract):
#   0 = Approved (job passes → merge allowed)
#   1 = Blocked  (job fails  → merge blocked)
#
# Global Gate Mode:
#   policy.yaml can set:
#     gate.mode: enforce | report_only
#
#   - enforce     : violations block merges (exit code 1)
#   - report_only : violations are reported but do NOT block (exit code 0)
# ==========================================================

import json
import yaml
from pathlib import Path

# ----------------------------------------------------------
# Engine imports
# ----------------------------------------------------------
# normalize(): Layer 1
#   - Converts raw tool outputs into standardized structures
#
# evaluate(): Layer 2
#   - Applies policy.yaml rules to normalized.json
#   - Produces evaluated.json and returns overall_block boolean
# ----------------------------------------------------------
from normalize import normalize
from policy_engine import evaluate

# ----------------------------------------------------------
# Output locations (relative to repo root)
# ----------------------------------------------------------
# All generated artifacts are written under quality/analysis/
# so that workflows can upload a single artifact folder.
# ----------------------------------------------------------
ANALYSIS_DIR = Path("quality/analysis")

# Policy file is read here ONLY for global gate.mode.
# Tool-specific policy evaluation remains inside policy_engine.py.
POLICY_FILE = Path("quality/ci/gate/policy.yaml")

# Machine-readable overall report (useful for downstream tooling)
# Mirrors evaluated.json (plus later, richer metadata if desired).
REPORT_FILE = ANALYSIS_DIR / "report.json"

# Human-readable markdown summary (PR-friendly)
SUMMARY_FILE = ANALYSIS_DIR / "summary.md"

# Evaluated output is written by policy_engine.py (or fail-safe writer)
EVALUATED_DIR = ANALYSIS_DIR / "evaluated"
EVALUATED_FILE = EVALUATED_DIR / "evaluated.json"


def generate_summary():
    """
    Generate a human-readable Markdown summary and a machine-readable JSON report.

    Inputs:
      quality/analysis/evaluated/evaluated.json
        - Produced by policy_engine.py (normal path)
        - OR written by write_failsafe_evaluated() (failure path)
        - Contains overall_block + per-tool evaluation results

    Outputs:
      quality/analysis/summary.md
        - Markdown table summarizing each tool's enforcement status
      quality/analysis/report.json
        - JSON copy of evaluated.json for machine consumption / audit artifacts

    IMPORTANT:
      - This function should never decide policy.
      - It should present what the policy engine decided.
    """
    # policy_engine.py (or the fail-safe writer) produces evaluated.json here
    evaluated_file = EVALUATED_FILE

    # If evaluated.json is missing, we cannot produce a reliable summary.
    # This indicates an unexpected orchestration failure.
    if not evaluated_file.exists():
        print("No evaluated results found.")
        return

    # Load evaluated results produced by the policy engine (or fail-safe writer).
    with open(evaluated_file) as f:
        data = json.load(f)

    # overall_block is the single gate decision for the workflow.
    overall_block = data["overall_block"]

    # results is the per-tool list containing blocking/policy_violation/reason and normalized payload.
    results = data["results"]

    # Build a compact Markdown summary designed for PR-friendly viewing.
    lines = []
    lines.append("# CareConnect Quality Gate Report")
    lines.append("")

    # Top-level status header (single source of truth is overall_block).
    if overall_block:
        lines.append("## ❌ MERGE BLOCKED")
    else:
        lines.append("## ✅ MERGE APPROVED")

    lines.append("")
    lines.append("| Tool | Blocking | Violation | Reason |")
    lines.append("|------|----------|-----------|--------|")

    # Each row shows:
    # - Tool key (matches policy.yaml / parser name)
    # - Blocking: whether this tool can block merges
    # - Violation: whether policy rules were violated
    # - Reason: normalized reason code set by policy_engine (or '-' if clean)
    for r in results:
        lines.append(
            f"| {r['tool']} | "
            f"{'Yes' if r['blocking'] else 'No'} | "
            f"{'Yes' if r['policy_violation'] else 'No'} | "
            f"{r['reason'] or '-'} |"
        )

    # Ensure analysis directory exists before writing outputs.
    SUMMARY_FILE.parent.mkdir(parents=True, exist_ok=True)

    # Write markdown summary.
    with open(SUMMARY_FILE, "w") as f:
        f.write("\n".join(lines))

    # Write machine-readable report (currently mirrors evaluated.json).
    with open(REPORT_FILE, "w") as f:
        json.dump(data, f, indent=2)

    print(f"Summary written to {SUMMARY_FILE}")
    print(f"Report written to {REPORT_FILE}")


def load_gate_mode():
    """
    Read gate.mode from policy.yaml.

    Returns:
      "enforce" or "report_only"

    Defaults:
      - If gate.mode is missing, malformed, or policy.yaml cannot be read,
        default to "enforce" (fail-safe).
    """
    try:
        with open(POLICY_FILE) as f:
            data = yaml.safe_load(f) or {}

        # gate section is optional; default to enforce behavior.
        mode = (data.get("gate", {}) or {}).get("mode", "enforce")
        mode = str(mode).strip().lower()

        # Only allow known modes; anything else is treated as enforce.
        return mode if mode in {"enforce", "report_only"} else "enforce"

    except Exception:
        # If policy cannot be read, default to enforce (fail safe).
        return "enforce"


def write_failsafe_evaluated(stage: str, error: Exception):
    """
    Write a minimal evaluated.json when normalization or policy evaluation fails.

    Why this exists:
      - CI governance requires determinism.
      - If a toolchain fails (normalization/parsing, policy read, etc.),
        we still need an auditable artifact explaining why the gate failed.
      - This guarantees summary.md and report.json can still be generated,
        even when upstream steps crash.

    Behavior:
      - Writes quality/analysis/evaluated/evaluated.json
      - Sets overall_block=True (governance blocks on system failure)
      - Creates a synthetic tool entry "gate_engine" describing the failure

    Args:
      stage: Where the failure occurred ("normalization" or "policy_evaluation")
      error: The exception raised
    """
    # Ensure evaluated folder exists.
    EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

    # Minimal evaluated payload consistent with policy_engine output schema.
    data = {
        # Fail-safe default: governance blocks on system failure
        "overall_block": True,
        "results": [
            {
                # Synthetic tool name used to represent orchestration failures
                "tool": "gate_engine",
                "blocking": True,
                "policy_violation": True,

                # Stable reason code used in the markdown summary
                "reason": f"{stage}_failed",

                # A normalized-shaped payload so downstream consumers stay stable
                "normalized": {
                    "tool": "gate_engine",
                    "executed": False,
                    "runtime_error": True,

                    # Non-zero count so it is unambiguously treated as "failure"
                    "violation_count": 1,

                    # Treat orchestration failures as high severity by default
                    "severity_counts": {
                        "critical": 0,
                        "high": 1,
                        "medium": 0,
                        "low": 0,
                        "info": 0,
                    },
                    "max_severity": "high",

                    # Store diagnostic context for debugging
                    "metadata": {
                        "stage": stage,
                        "error": str(error),
                    },
                },
            }
        ],
    }

    # Write fail-safe evaluated.json so generate_summary() can still run.
    with open(EVALUATED_FILE, "w") as f:
        json.dump(data, f, indent=2)

    print(f"⚠️ Wrote fail-safe evaluated results to {EVALUATED_FILE}")


def main():
    """
    Main CI entrypoint.

    Production-grade guarantees:
      - The gate attempts normalization and policy evaluation.
      - If either fails, a fail-safe evaluated.json is written.
      - summary.md and report.json are still generated for debugging/audit.
      - Final exit code honors gate.mode (enforce vs report_only).

    Enforcement contract:
      - In enforce mode, failures or violations block the merge (exit 1).
      - In report_only mode, failures or violations are reported but do not block (exit 0).
    """
    # Read global gate mode at the very beginning so failure paths still honor it.
    mode = load_gate_mode()

    # Pessimistic default; overwritten on successful evaluation.
    blocked = True

    # ----------------------------------------------------------
    # Layer 1: Normalization
    # ----------------------------------------------------------
    # Converts raw artifacts into normalized.json.
    # Any exception here results in a fail-safe evaluated.json.
    try:
        print("Running normalization...")
        normalize()
    except Exception as e:
        print(f"❌ Normalization failed: {e}")
        write_failsafe_evaluated(stage="normalization", error=e)

        # Still generate outputs from the fail-safe evaluated.json.
        print("Generating summary (fail-safe)...")
        generate_summary()

        # Honor global gate mode.
        if mode == "report_only":
            print("⚠️ gate.mode=report_only → not blocking merge despite gate failure.")
            exit(0)

        print("❌ Merge blocked due to normalization failure.")
        exit(1)

    # ----------------------------------------------------------
    # Layer 2: Policy Evaluation
    # ----------------------------------------------------------
    # Applies policy.yaml rules to normalized.json and returns overall block decision.
    # Any exception here results in a fail-safe evaluated.json.
    try:
        print("Applying policy rules...")
        blocked = evaluate()
    except Exception as e:
        print(f"❌ Policy evaluation failed: {e}")
        write_failsafe_evaluated(stage="policy_evaluation", error=e)

        # Still generate outputs from the fail-safe evaluated.json.
        print("Generating summary (fail-safe)...")
        generate_summary()

        # Honor global gate mode.
        if mode == "report_only":
            print("⚠️ gate.mode=report_only → not blocking merge despite gate failure.")
            exit(0)

        print("❌ Merge blocked due to policy evaluation failure.")
        exit(1)

    # ----------------------------------------------------------
    # Reporting (normal path)
    # ----------------------------------------------------------
    # At this point evaluated.json should exist, so we generate summary/report outputs.
    print("Generating summary...")
    generate_summary()

    # ----------------------------------------------------------
    # Final enforcement decision (normal path)
    # ----------------------------------------------------------
    # report_only:
    #   - Always pass CI, but preserve visibility via report artifacts.
    if mode == "report_only":
        if blocked:
            print("⚠️ Policy violations detected, but gate.mode=report_only so merge is NOT blocked.")
        else:
            print("✅ Merge approved (gate.mode=report_only).")
        exit(0)

    # enforce (default):
    #   - Block merges when policy violations exist.
    if blocked:
        print("❌ Merge blocked due to policy violations.")
        exit(1)

    print("✅ Merge approved.")
    exit(0)


# Allow local execution: python quality/ci/gate/gate.py
if __name__ == "__main__":
    main()