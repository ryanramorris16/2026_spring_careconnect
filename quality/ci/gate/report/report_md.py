# File: quality/ci/gate/report/report_md.py
# ==========================================================
# Markdown Report Builder
# ----------------------------------------------------------
# Builds the markdown report string consumed by:
#   - GitHub Actions Job Summary
#   - PR comment
#
# Functions:
#   build_markdown_report(evaluated_doc, env) → str
# ==========================================================

from datetime import datetime, timezone
from quality.ci.gate.report.report_constants import (
    CATEGORY_MAP,
    _MD_TABLE_HEADER,
    _MD_TABLE_SEPARATOR,
)

PR_COMMENT_MARKER = "## 🔍 CareConnect — Security & Quality Analysis Report"


def build_markdown_report(evaluated_doc: dict, env: dict) -> str:
    overall_block        = bool(evaluated_doc.get("overall_block", True))
    blocking_results     = evaluated_doc.get("blocking_results", [])
    non_blocking_results = evaluated_doc.get("non_blocking_results", [])
    all_results          = blocking_results + non_blocking_results

    sha_short    = env["sha"][:7] if env["sha"] else "unknown"
    run_url      = (f"{env['server_url']}/{env['repository']}"
                    f"/actions/runs/{env['run_id']}")
    commit_url   = (f"{env['server_url']}/{env['repository']}"
                    f"/commit/{env['sha']}")
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    lines: list[str] = []

    # Title and status banner
    lines += [
        "# CareConnect Quality Gate Report",
        "",
        ("> **🚫 BLOCKED** — One or more required checks failed. "
         "Fix the issues below before merging."
         if overall_block else
         "> **✅ APPROVED** — All required checks passed."),
        "",
        PR_COMMENT_MARKER,
        "",
    ]

    # Report Header
    lines += [
        "### 📋 Report Header",
        "",
        _MD_TABLE_HEADER,
        _MD_TABLE_SEPARATOR,
        f"| **Generated (UTC)** | {generated_at} |",
        f"| **Pipeline Run** | [#{env['run_number']}]({run_url}) |",
        f"| **Trigger** | `{env['event_name']}` |",
        f"| **Scan Root** | `{env['scan_root']}` |",
        "",
        "_All timestamps are reported in Coordinated Universal Time (UTC)._",
        "",
    ]

    # PR section
    if env["event_name"] == "pull_request" and env["pr_number"]:
        lines += [
            "### 🔀 Pull Request",
            "",
            _MD_TABLE_HEADER,
            _MD_TABLE_SEPARATOR,
            f"| **PR Number** | #{env['pr_number']} |",
            f"| **PR Author** | @{env['actor']} |",
            f"| **Source Branch** | `{env['head_ref']}` |",
            f"| **Target Branch** | `{env['base_ref']}` |",
            "",
        ]

    # Commit Details
    lines += [
        "### 📝 Commit Details",
        "",
        _MD_TABLE_HEADER,
        _MD_TABLE_SEPARATOR,
        f"| **Commit SHA** | `{sha_short}` ([full]({commit_url})) |",
        "",
    ]

    # Legend
    lines += [
        "### 📖 Legend",
        "",
        "| Symbol | Meaning |",
        "|--------|---------|",
        "| ✅ SUCCESS | Tool ran and found no violations |",
        "| ❌ FAILURE | Tool found one or more violations |",
        "| ⏸️ DISABLED | Tool is not yet configured |",
        "| 🚫 Enforced | Violations from this tool will block the merge |",
        "| ⚠️ Advisory | Violations are reported but will not block the merge |",
        "",
    ]

    # Tool Results Summary
    lines += [
        "### 🛡️ Tool Results Summary",
        "",
        "| Tool | Category | Status | Role | Findings |",
        "|------|----------|--------|------|----------|",
    ]

    for r in all_results:
        tool          = r.get("tool", "unknown")
        category      = CATEGORY_MAP.get(tool, "Analysis")
        violation     = r.get("policy_violation", False)
        blocking      = r.get("blocking", False)
        reason        = r.get("reason", "")
        normalized    = r.get("normalized", {})
        finding_count = normalized.get("violation_count", 0)
        findings_label = f"{finding_count} finding(s)" if finding_count else "—"

        if reason == "disabled":
            status = "⏸️ DISABLED"
        elif violation:
            status = "❌ FAILURE"
        else:
            status = "✅ SUCCESS"

        role = "🚫 Enforced" if blocking else "⚠️ Advisory"
        lines.append(
            f"| {tool} | {category} | {status} | {role} | {findings_label} |"
        )

    lines += [
        "",
        "---",
        "_Full artifact bundle available in the workflow run artifacts._",
        "",
    ]

    return "\n".join(lines)
