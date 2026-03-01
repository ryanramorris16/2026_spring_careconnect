# File: quality/ci/gate/normalize.py
# ==========================================================
# normalize.py
# ----------------------------------------------------------
# Normalization Layer (Layer 1 of the Quality Gate Engine)
#
# Purpose:
#   Convert heterogeneous tool outputs (XML/JSON/JSONL) into a
#   single consistent schema so policy evaluation can be simple
#   and deterministic.
#
# Inputs:
#   Raw tool artifacts read from:
#     quality/analysis/raw/
#
# Output:
#   A single normalized file written to:
#     quality/analysis/normalized/normalized.json
#
# normalized.json top-level structure:
#   {
#     "generated_at":    "2026-02-24T12:00:00Z",  ← UTC timestamp
#     "tool_count":      8,                        ← total tools registered
#     "total_violations": 42,                      ← sum across all tools
#     "max_severity":    "critical",               ← highest across all tools
#     "results": [                                 ← full per-tool detail
#       {
#         "tool":             "trufflehog",
#         "artifact_present": true,
#         "executed":         true,
#         "runtime_error":    false,
#         "findings":         [ ... ],
#         "violation_count":  3,
#         "severity_counts":  { "critical": 1, "high": 2, ... },
#         "max_severity":     "critical",
#         "metadata":         { ... }
#       },
#       ...
#     ]
#   }
#
# Design Rules:
#   - DOES NOT apply policy thresholds (policy_engine.py owns that).
#   - DOES NOT decide pass/fail (gate.py owns that).
#   - MUST be resilient: one parser crash must not prevent other
#     tools from being collected and normalized.
#   - A parser crash is recorded as runtime_error=True for that tool,
#     which the policy engine will treat as a governance violation.
#
# Execution:
#   Recommended:  python -m quality.ci.gate.normalize
#   Direct:       python quality/ci/gate/normalize.py
# ==========================================================

import json
from datetime import datetime, timezone
from pathlib import Path

from .parsers.bandit import parse_bandit
from .parsers.checkstyle import parse_checkstyle
from .parsers.dependency_check import parse_dependency_check
from .parsers.flutter import parse_flutter
from .parsers.gitleaks import parse_gitleaks
from .parsers.htmlhint import parse_htmlhint
from .parsers.pmd import parse_pmd
from .parsers.pylint import parse_pylint
from .parsers.semgrep import parse_semgrep
from .parsers.sonar import parse_sonar
from .parsers.spotbugs import parse_spotbugs
from .parsers.stylelint import parse_stylelint
from .parsers.trufflehog import parse_trufflehog
from .schemas import base_tool_result
from .utils import determine_max_severity, SEVERITY_ORDER

# ----------------------------------------------------------
# Directory configuration
# ----------------------------------------------------------
RAW_DIR        = Path("quality/analysis/raw")
NORMALIZED_DIR = Path("quality/analysis/normalized")
OUTPUT_FILE    = NORMALIZED_DIR / "normalized.json"

# ----------------------------------------------------------
# Tool registration
# ----------------------------------------------------------
# Each entry maps a tool key to its parser function.
#
# tool_key MUST match the key used in:
#   - policy.yaml  (tools.<tool_key>)
#   - evaluated.json
#   - report.md
#
# Order determines the order of results in normalized.json
# and the final report.
# ----------------------------------------------------------
TOOL_PARSERS: list[tuple[str, callable]] = [
    ("trufflehog",        parse_trufflehog),       # Secrets detection      (JSONL)
    ("gitleaks",          parse_gitleaks),          # Secrets detection      (JSON)
    ("flutter_analyze",   parse_flutter),           # Dart static analysis   (JSON)
    ("checkstyle",        parse_checkstyle),        # Java style enforcement (XML)
    ("pmd",               parse_pmd),               # Java source analysis   (XML)
    ("spotbugs",          parse_spotbugs),          # Java bytecode analysis (XML)
    ("semgrep",           parse_semgrep),           # Multi-language SAST    (JSON)
    ("pylint",            parse_pylint),            # Python static analysis (JSON)
    ("bandit",            parse_bandit),            # Python security SAST   (JSON)
    ("htmlhint",          parse_htmlhint),          # HTML static analysis   (JSON)
    ("stylelint",         parse_stylelint),         # CSS/SCSS analysis      (JSON)
    ("dependency_check",  parse_dependency_check),  # SCA — known CVEs       (JSON)
    ("sonar",             parse_sonar),             # Quality gate           (JSON)
]


def normalize() -> list[dict]:
    """
    Run all registered tool parsers and write normalized.json.

    Each parser reads its raw artifact from RAW_DIR and returns a
    standardized result dict conforming to schemas.base_tool_result.

    A top-level summary wrapper is written around all tool results
    so the policy engine and report layer have a single entry point
    for overall pipeline state.

    Returns:
        The list of per-tool result dicts (also written to disk).

    Contract:
        - Always attempts to run every registered parser.
        - Never aborts on a single tool failure.
        - Parser exceptions are converted to runtime_error records.
        - Output directory is created if it does not exist.
    """
    NORMALIZED_DIR.mkdir(parents=True, exist_ok=True)

    results: list[dict] = []

    # ----------------------------------------------------------
    # Run each parser
    # ----------------------------------------------------------
    # Parsers are isolated — a crash in one does not stop others.
    # Any uncaught exception is captured and converted to a
    # runtime_error result so the policy engine can flag it.
    # ----------------------------------------------------------
    for tool_name, parser in TOOL_PARSERS:
        try:
            result = parser(RAW_DIR)
            results.append(result)

        except Exception as e:
            error_result = base_tool_result(tool_name)
            error_result["executed"]          = False
            error_result["runtime_error"]     = True
            error_result["metadata"]["error"] = (
                f"Parser raised an unhandled exception: {e}"
            )
            results.append(error_result)

    # ----------------------------------------------------------
    # Build top-level summary
    # ----------------------------------------------------------
    total_violations = sum(r.get("violation_count", 0) for r in results)

    combined_severity_counts: dict[str, int] = dict.fromkeys(SEVERITY_ORDER, 0)
    for r in results:
        for level, count in r.get("severity_counts", {}).items():
            if level in combined_severity_counts:
                combined_severity_counts[level] += count

    overall_max_severity = determine_max_severity(combined_severity_counts)
    generated_at         = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    normalized_doc = {
        "generated_at":     generated_at,
        "tool_count":       len(results),
        "total_violations": total_violations,
        "max_severity":     overall_max_severity,
        "results":          results,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(normalized_doc, f, indent=2)

    print(f"[normalize] {len(results)} tool(s) processed.")
    print(f"[normalize] Total violations : {total_violations}")
    print(f"[normalize] Max severity     : {overall_max_severity or 'none'}")
    print(f"[normalize] Output written to: {OUTPUT_FILE}")

    return results


if __name__ == "__main__":
    normalize()
