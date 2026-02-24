# File: quality/ci/gate/gate.py
# ==========================================================
# gate.py
# ----------------------------------------------------------
# Quality Gate Orchestrator (Single Enforcement Authority)
#
# This is the ONLY file that controls the final CI exit code.
#
# Layer Architecture:
#   Layer 1 — normalize.py
#     Reads raw tool artifacts from quality/analysis/raw/.
#     Produces quality/analysis/normalized/normalized.json.
#
#   Layer 2 — policy_engine.py
#     Reads normalized.json + policy.yaml.
#     Produces quality/analysis/evaluated/evaluated.json.
#     Determines overall_block (True/False).
#
#   Layer 3 — humanize.py
#     Reads normalized.json + evaluated.json.
#     Produces quality/analysis/human/index.md and per-tool pages.
#
#   Layer 4 — report.py (called separately by the workflow)
#     Reads evaluated.json.
#     Produces quality/analysis/report.md.
#     Posts/updates the PR comment via GitHub API.
#
# Responsibilities:
#   1) Invoke Layers 1–3 in sequence.
#   2) Write fail-safe evaluated.json if any layer crashes.
#   3) Honor gate.mode (enforce vs report_only).
#   4) Exit with the correct status code.
#
# Exit Codes (CI Enforcement Contract):
#   0 → Approved  (merge allowed)
#   1 → Blocked   (merge blocked)
#
# Gate Modes (defined in policy.yaml → gate.mode):
#   enforce     → violations block the merge (default, fail-safe)
#   report_only → violations are reported but do NOT block
#
# Design Rules:
#   - Tool logic belongs in parsers/.
#   - Policy thresholds belong in policy.yaml.
#   - Report rendering belongs in report.py (called by workflow).
#   - This file coordinates only — no tool or policy logic here.
#   - Fail-safe: any unhandled error results in exit code 1.
#
# Execution:
#   python -m quality.ci.gate.gate
# ==========================================================

import json
import yaml
from pathlib import Path

from .humanize import generate_human_readable_outputs
from .normalize import normalize
from .policy_engine import evaluate

# ----------------------------------------------------------
# Path Configuration
# ----------------------------------------------------------
ANALYSIS_DIR    = Path("quality/analysis")
POLICY_FILE     = Path("quality/ci/gate/policy.yaml")
EVALUATED_DIR   = ANALYSIS_DIR / "evaluated"
EVALUATED_FILE  = EVALUATED_DIR / "evaluated.json"
NORMALIZED_FILE = ANALYSIS_DIR / "normalized" / "normalized.json"


# ==========================================================
# Gate Mode
# ==========================================================

def _load_gate_mode() -> str:
    """
    Read gate.mode from policy.yaml.

    Returns:
        "enforce"     → violations block the merge (default).
        "report_only" → violations are reported but do not block.

    Fail-safe: any error reading policy.yaml defaults to "enforce".
    """
    try:
        with open(POLICY_FILE, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}

        mode = str(
            (data.get("gate", {}) or {}).get("mode", "enforce")
        ).strip().lower()

        return mode if mode in {"enforce", "report_only"} else "enforce"

    except Exception:
        # Cannot read policy — default to enforce (fail-safe)
        return "enforce"


# ==========================================================
# Fail-Safe Evaluated Writer
# ==========================================================

def _write_failsafe_evaluated(stage: str, error: Exception) -> None:
    """
    Write a minimal evaluated.json when a pipeline layer crashes.

    Guarantees that evaluated.json always exists for report.py to
    consume, even when normalization or policy evaluation fails.

    The synthetic "gate_engine" tool entry documents the failure
    so the report clearly shows what went wrong and at which stage.

    Args:
        stage: Name of the failing stage (e.g. "normalization").
        error: The exception that was raised.
    """
    EVALUATED_DIR.mkdir(parents=True, exist_ok=True)

    # Build a synthetic tool entry that represents the gate failure
    gate_engine_entry = {
        "tool":             "gate_engine",
        "blocking":         True,
        "policy_violation": True,
        "reason":           f"{stage}_failed",
        "normalized": {
            "tool":             "gate_engine",
            "artifact_present": False,
            "executed":         False,
            "runtime_error":    True,
            "findings":         [],
            "violation_count":  1,
            "severity_counts": {
                "critical": 0,
                "high":     1,
                "medium":   0,
                "low":      0,
                "info":     0,
            },
            "max_severity": "high",
            "metadata": {
                "stage": stage,
                "error": str(error),
            },
        },
    }

    # Write using the current evaluated.json structure:
    # blocking_results and non_blocking_results are separate sections
    failsafe_doc = {
        "overall_block":        True,
        "generated_at":         "",
        "blocking_results":     [gate_engine_entry],
        "non_blocking_results": [],
    }

    EVALUATED_FILE.write_text(
        json.dumps(failsafe_doc, indent=2), encoding="utf-8"
    )
    print(f"[gate] Fail-safe evaluated.json written to: {EVALUATED_FILE}")


# ==========================================================
# Main Entrypoint
# ==========================================================

def main() -> None:
    """
    Quality Gate main entry point.

    Executes Layers 1–3 in sequence, handles failures at each stage,
    and exits with the correct status code based on gate.mode.

    Guarantees:
        - Always attempts all three layers.
        - Always produces evaluated.json (even on failure).
        - Always honors gate.mode for the final exit code.
        - Never exits with an unhandled exception.
    """
    mode    = _load_gate_mode()
    blocked = True  # Pessimistic default — fail-safe

    # ----------------------------------------------------------
    # Layer 1 — Normalization
    # ----------------------------------------------------------
    print("[gate] Layer 1: Running normalization...")
    try:
        normalize()
    except Exception as e:
        print(f"[gate] ❌ Normalization failed: {e}")
        _write_failsafe_evaluated("normalization", e)
        _exit(blocked=True, mode=mode)

    # ----------------------------------------------------------
    # Layer 2 — Policy Evaluation
    # ----------------------------------------------------------
    print("[gate] Layer 2: Applying policy rules...")
    try:
        blocked = evaluate()
    except Exception as e:
        print(f"[gate] ❌ Policy evaluation failed: {e}")
        _write_failsafe_evaluated("policy_evaluation", e)
        _exit(blocked=True, mode=mode)

    # ----------------------------------------------------------
    # Layer 3 — Human-Readable Report Pages
    # ----------------------------------------------------------
    # Failures here are non-fatal — enforcement is already decided.
    # ----------------------------------------------------------
    print("[gate] Layer 3: Generating human-readable pages...")
    try:
        generate_human_readable_outputs(
            repo_root=Path(".").resolve(),
            analysis_dir=ANALYSIS_DIR.resolve(),
            normalized_path=NORMALIZED_FILE.resolve(),
            evaluated_path=EVALUATED_FILE.resolve(),
        )
    except Exception as e:
        # Reporting failure must never block enforcement
        print(f"[gate] ⚠️ Human-readable report generation failed (non-fatal): {e}")

    # ----------------------------------------------------------
    # Final Enforcement Decision
    # ----------------------------------------------------------
    _exit(blocked=blocked, mode=mode)


def _exit(blocked: bool, mode: str) -> None:
    """
    Apply gate.mode and exit with the correct status code.

    Args:
        blocked: True if any blocking tool violated its policy.
        mode:    "enforce" or "report_only".

    Exit codes:
        0 → merge allowed
        1 → merge blocked
    """
    if mode == "report_only":
        if blocked:
            print("[gate] ⚠️ Violations detected — report_only mode, merge not blocked.")
        else:
            print("[gate] ✅ All checks passed (report_only mode).")
        raise SystemExit(0)

    if blocked:
        print("[gate] ❌ Merge blocked due to policy violations.")
        raise SystemExit(1)

    print("[gate] ✅ All checks passed. Merge approved.")
    raise SystemExit(0)


if __name__ == "__main__":
    main()
    