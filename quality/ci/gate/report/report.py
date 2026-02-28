# File: quality/ci/gate/report/report.py
# ==========================================================
# Report Generation — Entry Point (Layer 4)
# ----------------------------------------------------------
# Reads evaluated.json and normalized.json and produces:
#   quality/analysis/report.md    → PR comment + job summary
#   quality/analysis/report.html  → rich downloadable report
#
# Delegates to:
#   report_md.py     → markdown builder
#   report_html.py   → HTML builder
#   report_github.py → PR comment poster
#
# Execution:
#   python -m quality.ci.gate.report.report
# ==========================================================

import json
import os
import sys
from pathlib import Path

from quality.ci.gate.report.report_md     import build_markdown_report
from quality.ci.gate.report.report_html   import build_html_report
from quality.ci.gate.report.report_github import post_or_update_pr_comment

# ----------------------------------------------------------
# Path Configuration
# ----------------------------------------------------------
ANALYSIS_DIR     = Path("quality/analysis")
EVALUATED_FILE   = ANALYSIS_DIR / "evaluated" / "evaluated.json"
NORMALIZED_FILE  = ANALYSIS_DIR / "normalized" / "normalized.json"
REPORT_MD_FILE   = ANALYSIS_DIR / "report.md"
REPORT_HTML_FILE = ANALYSIS_DIR / "report.html"


def _get_env() -> dict:
    return {
        "event_name": os.environ.get("GITHUB_EVENT_NAME", "unknown"),
        "run_number": os.environ.get("GITHUB_RUN_NUMBER", "?"),
        "run_id":     os.environ.get("GITHUB_RUN_ID", ""),
        "sha":        os.environ.get("GITHUB_SHA", ""),
        "server_url": os.environ.get("GITHUB_SERVER_URL", "https://github.com"),
        "repository": os.environ.get("GITHUB_REPOSITORY", ""),
        "actor":      os.environ.get("GITHUB_ACTOR", "unknown"),
        "head_ref":   os.environ.get("GITHUB_HEAD_REF", ""),
        "base_ref":   os.environ.get("GITHUB_BASE_REF", ""),
        "pr_number":  os.environ.get("PR_NUMBER", ""),
        "scan_root":  os.environ.get("SCAN_ROOT", "."),
        "token":      os.environ.get("GITHUB_TOKEN", ""),
    }


def generate_report() -> None:
    env = _get_env()

    if not EVALUATED_FILE.exists():
        print(f"[report] ❌ evaluated.json not found at {EVALUATED_FILE}.")
        sys.exit(1)

    try:
        with open(EVALUATED_FILE, "r", encoding="utf-8") as f:
            evaluated_doc = json.load(f)
    except Exception as e:
        print(f"[report] ❌ Failed to parse evaluated.json: {e}")
        sys.exit(1)

    REPORT_MD_FILE.parent.mkdir(parents=True, exist_ok=True)

    md_body = build_markdown_report(evaluated_doc, env)
    REPORT_MD_FILE.write_text(md_body, encoding="utf-8")
    print(f"[report] report.md written to: {REPORT_MD_FILE}")

    html_body = build_html_report(evaluated_doc, env)
    REPORT_HTML_FILE.write_text(html_body, encoding="utf-8")
    print(f"[report] report.html written to: {REPORT_HTML_FILE}")

    if env["event_name"] == "pull_request" and env["pr_number"]:
        post_or_update_pr_comment(md_body, env)
    else:
        print("[report] Not a PR event — skipping PR comment.")


if __name__ == "__main__":
    generate_report()
