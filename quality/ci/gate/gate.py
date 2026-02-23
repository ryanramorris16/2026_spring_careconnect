# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/gate.py
# ==========================================================
# gate.py
# ----------------------------------------------------------
# Quality Gate Orchestrator (Single Enforcement Authority)
#
# This is the ONLY file that controls the final CI exit code.
#
# Architecture Overview:
#
#   Layer 1 — Normalization (normalize.py)
#     - Reads raw tool artifacts from quality/analysis/raw/
#     - Produces normalized.json in quality/analysis/normalized/
#
#   Layer 2 — Policy Evaluation (policy_engine.py)
#     - Reads normalized.json + policy.yaml
#     - Produces evaluated.json
#     - Determines overall_block (True/False)
#
#   Layer 3 — Human Readable Reporting (humanize.py)
#     - Produces Markdown breakdowns in quality/analysis/human/
#
# Responsibilities of gate.py:
#   1) Invoke normalization
#   2) Invoke policy evaluation
#   3) Generate human + machine readable outputs
#   4) Exit with correct status code (controls merge gating)
#
# Exit Codes (CI Enforcement Contract):
#   0 = Approved (job passes → merge allowed)
#   1 = Blocked  (job fails  → merge blocked)
#
# Global Gate Mode:
#   policy.yaml may define:
#       gate.mode: enforce | report_only
#
#   - enforce     : violations block merges
#   - report_only : violations are reported but do NOT block
#
# DESIGN PRINCIPLES:
# - Tool logic belongs in parsers/
# - Policy thresholds belong in policy.yaml
# - This file coordinates, but does NOT embed tool logic
# - Fail-safe behavior always blocks in enforce mode
# ==========================================================

from __future__ import annotations

import json
import yaml
from pathlib import Path

# ----------------------------------------------------------
# Engine Imports (Relative for Package Stability)
# ----------------------------------------------------------
# Using relative imports ensures stability when executed via:
#   python -m quality.ci.gate.gate
# ----------------------------------------------------------
from .normalize import normalize
from .policy_engine import evaluate
from .humanize import generate_human_readable_outputs

# ----------------------------------------------------------
# Directory Configuration
# ----------------------------------------------------------
ANALYSIS_DIR = Path("quality/analysis")
POLICY_FILE = Path("quality/ci/gate/policy.yaml")

REPORT_FILE = ANALYSIS_DIR / "report.json"
SUMMARY_FILE = ANALYSIS_DIR / "summary.md"

EVALUATED_DIR = ANALYSIS_DIR / "evaluated"
EVALUATED_FILE = EVALUATED_DIR / "evaluated.json"

NORMALIZED_FILE = ANALYSIS_DIR / "normalized/normalized.json"


# ==========================================================
# Reporting
# ==========================================================

def generate_summary() -> None:
    """
    Generate:
      - summary.md (human-readable)
      - report.json (machine-readable)

    Uses evaluated.json as single source of truth.
    """

    if not EVALUATED_FILE.exists():
        print("⚠️ No evaluated results found. Cannot generate summary.")
        return

    with open(EVALUATED_FILE, "r", encoding="utf-8") as f:
        data = json.load(f)

    overall_block = bool(data.get("overall_block", True))
    results = data.get("results", [])

    lines = [
        "# CareConnect Quality Gate Report",
        "",
        "## ❌ MERGE BLOCKED" if overall_block else "## ✅ MERGE APPROVED",
        "",
        "| Tool | Blocking | Violation | Reason |",
        "|------|----------|-----------|--------|",
    ]

    for r in results:
        lines.append(
            f"| {r.get('tool')} | "
            f"{'Yes' if r.get('blocking') else 'No'} | "
            f"{'Yes' if r.get('policy_violation') else 'No'} | "
            f"{r.get('reason') or '-'} |"
        )

    SUMMARY_FILE.parent.mkdir(parents=True, exist_ok=True)

    SUMMARY_FILE.write_text("\n".join(lines), encoding="utf-8")
    REPORT_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")

    print(f"Summary written to {SUMMARY_FILE}")
    print(f"Report written to {REPORT_FILE}")


# ==========================================================
# Gate Mode Handling
# ==========================================================

def load_gate_mode() -> str:
    """
    Read gate.mode from policy.yaml.

    Defaults:
      - enforce (fail-safe)

    Returns:
      "enforce" or "report_only"
    """
    try:
        with open(POLICY_FILE, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}

        mode = str(
            (data.get("gate", {}) or {}).get("mode", "enforce")
        ).strip().lower()

        return mode if mode in {"enforce", "report_only"} else "enforce"

    except Exception:
        # Fail-safe default
        return "enforce"


# ==========================================================
# Fail-Safe Evaluated Writer
# ==========================================================

def write_failsafe_evaluated(stage: str, error: Exception) -> None:
    """
    Write minimal evaluated.json when normalization or policy fails.

    Guarantees:
      - evaluated.json always exists
      - overall_block=True (fail-safe)
      - Synthetic tool entry "gate_engine" records failure
    """

    EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

    data = {
        "overall_block": True,
        "results": [
            {
                "tool": "gate_engine",
                "blocking": True,
                "policy_violation": True,
                "reason": f"{stage}_failed",
                "normalized": {
                    "tool": "gate_engine",
                    "artifact_present": False,
                    "executed": False,
                    "runtime_error": True,
                    "violation_count": 1,
                    "severity_counts": {
                        "critical": 0,
                        "high": 1,
                        "medium": 0,
                        "low": 0,
                        "info": 0,
                    },
                    "max_severity": "high",
                    "findings": [],
                    "metadata": {
                        "stage": stage,
                        "error": str(error),
                    },
                },
            }
        ],
    }

    EVALUATED_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")
    print(f"⚠️ Wrote fail-safe evaluated results to {EVALUATED_FILE}")


# ==========================================================
# Main Entrypoint
# ==========================================================

def main() -> None:
    """
    Main CI Entry Point.

    Guarantees:
      - Always attempts normalization
      - Always attempts policy evaluation
      - Writes evaluated.json in all cases
      - Generates summary/report artifacts
      - Honors gate.mode
    """

    mode = load_gate_mode()
    blocked = True  # pessimistic default

    # ------------------------------------------------------
    # Layer 1 — Normalization
    # ------------------------------------------------------
    try:
        print("Running normalization...")
        normalize()
    except Exception as e:
        print(f"❌ Normalization failed: {e}")
        write_failsafe_evaluated("normalization", e)
        generate_summary()

        if mode == "report_only":
            print("⚠️ report_only mode → not blocking merge.")
            raise SystemExit(0)

        raise SystemExit(1)

    # ------------------------------------------------------
    # Layer 2 — Policy Evaluation
    # ------------------------------------------------------
    try:
        print("Applying policy rules...")
        blocked = evaluate()
    except Exception as e:
        print(f"❌ Policy evaluation failed: {e}")
        write_failsafe_evaluated("policy_evaluation", e)
        generate_summary()

        if mode == "report_only":
            print("⚠️ report_only mode → not blocking merge.")
            raise SystemExit(0)

        raise SystemExit(1)

    # ------------------------------------------------------
    # Layer 3 — Human Readable Reports
    # ------------------------------------------------------
    try:
        generate_human_readable_outputs(
            repo_root=Path(".").resolve(),
            analysis_dir=ANALYSIS_DIR.resolve(),
            normalized_path=NORMALIZED_FILE.resolve(),
            evaluated_path=EVALUATED_FILE.resolve(),
        )
    except Exception as e:
        # Reporting should never block enforcement.
        print(f"⚠️ Human-readable report generation failed: {e}")

    # ------------------------------------------------------
    # Summary + Machine Report
    # ------------------------------------------------------
    generate_summary()

    # ------------------------------------------------------
    # Final Enforcement Decision
    # ------------------------------------------------------
    if mode == "report_only":
        if blocked:
            print("⚠️ Violations detected, but report_only mode enabled.")
        else:
            print("✅ Merge approved (report_only mode).")
        raise SystemExit(0)

    if blocked:
        print("❌ Merge blocked due to policy violations.")
        raise SystemExit(1)

    print("✅ Merge approved.")
    raise SystemExit(0)


# Allow local execution:
#   python -m quality.ci.gate.gate
if __name__ == "__main__":
    main()
    