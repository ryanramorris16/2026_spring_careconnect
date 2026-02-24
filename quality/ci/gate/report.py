# File: quality/ci/gate/report.py
# ==========================================================
# report.py
# ----------------------------------------------------------
# Report Generation Layer (Layer 4 of the Quality Gate Engine)
#
# Purpose:
#   Read evaluated.json and normalized.json and produce:
#     - quality/analysis/report.md   → PR comment + job summary
#     - quality/analysis/report.html → rich downloadable report
#
# Inputs:
#   quality/analysis/evaluated/evaluated.json
#     Produced by policy_engine.py (Layer 2).
#     Contains blocking_results[] and non_blocking_results[].
#
#   quality/analysis/normalized/normalized.json
#     Produced by normalize.py (Layer 1).
#     Contains full findings[] per tool.
#
# Outputs:
#   quality/analysis/report.md
#     High-level summary consumed by:
#       - GitHub Actions Job Summary (workflow Step 9)
#       - PR comment (posted/updated by this script)
#
#   quality/analysis/report.html
#     Rich self-contained HTML report consumed by:
#       - Downloadable artifact bundle (workflow Step 11)
#     Includes:
#       - Header and summary table (same as report.md)
#       - Blocking tools section with full per-finding detail
#       - Non-blocking tools section with full per-finding detail
#       - Inline CSS — no external dependencies, opens in any browser
#
# PR Comment:
#   - Posted on pull_request events only.
#   - One comment per PR, updated in place (no spam).
#   - Identified by PR_COMMENT_MARKER string in the comment body.
#
# Environment Variables (set by workflow):
#   GITHUB_TOKEN        → GitHub API authentication
#   GITHUB_EVENT_NAME   → "pull_request" or "push"
#   GITHUB_REPOSITORY   → "owner/repo"
#   GITHUB_RUN_ID       → workflow run ID for deep links
#   GITHUB_RUN_NUMBER   → human-readable run number
#   GITHUB_SHA          → full commit SHA
#   GITHUB_SERVER_URL   → e.g. https://github.com
#   GITHUB_HEAD_REF     → source branch (PR only)
#   GITHUB_BASE_REF     → target branch (PR only)
#   GITHUB_ACTOR        → user who triggered the workflow
#   SCAN_ROOT           → the directory that was scanned
#   PR_NUMBER           → pull request number (PR only)
#
# Execution:
#   python -m quality.ci.gate.report
# ==========================================================

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

import requests

# ----------------------------------------------------------
# Path Configuration
# ----------------------------------------------------------
ANALYSIS_DIR   = Path("quality/analysis")
EVALUATED_FILE = ANALYSIS_DIR / "evaluated" / "evaluated.json"
NORMALIZED_FILE = ANALYSIS_DIR / "normalized" / "normalized.json"
REPORT_MD_FILE  = ANALYSIS_DIR / "report.md"
REPORT_HTML_FILE = ANALYSIS_DIR / "report.html"

# ----------------------------------------------------------
# Stable PR comment marker
# ----------------------------------------------------------
# Used to identify the existing bot comment for in-place updates.
# Must never change between runs.
# ----------------------------------------------------------
PR_COMMENT_MARKER = "## 🔍 CareConnect — Security & Quality Analysis Report"

# ----------------------------------------------------------
# Tool category map
# ----------------------------------------------------------
CATEGORY_MAP: dict[str, str] = {
    "trufflehog":       "Secrets Scan",
    "checkstyle":       "SAST — Java",
    "pmd":              "SAST — Java",
    "spotbugs":         "SAST — Java",
    "semgrep":          "SAST — Multi",
    "flutter_analyze":  "SAST — Flutter",
    "dependency_check": "SCA — Multi",
    "sonar":            "Quality Gate",
}

# ----------------------------------------------------------
# Severity badge colors (used in HTML report)
# ----------------------------------------------------------
SEVERITY_COLORS: dict[str, str] = {
    "critical": "#7c0000",
    "high":     "#c0392b",
    "medium":   "#e67e22",
    "low":      "#f1c40f",
    "info":     "#3498db",
}


# ==========================================================
# Shared Environment Helper
# ==========================================================

def _get_env() -> dict:
    """
    Collect all workflow environment variables into a single dict.
    Provides safe defaults for local execution outside of CI.
    """
    return {
        "event_name":  os.environ.get("GITHUB_EVENT_NAME", "unknown"),
        "run_number":  os.environ.get("GITHUB_RUN_NUMBER", "?"),
        "run_id":      os.environ.get("GITHUB_RUN_ID", ""),
        "sha":         os.environ.get("GITHUB_SHA", ""),
        "server_url":  os.environ.get("GITHUB_SERVER_URL", "https://github.com"),
        "repository":  os.environ.get("GITHUB_REPOSITORY", ""),
        "actor":       os.environ.get("GITHUB_ACTOR", "unknown"),
        "head_ref":    os.environ.get("GITHUB_HEAD_REF", ""),
        "base_ref":    os.environ.get("GITHUB_BASE_REF", ""),
        "pr_number":   os.environ.get("PR_NUMBER", ""),
        "scan_root":   os.environ.get("SCAN_ROOT", "."),
        "token":       os.environ.get("GITHUB_TOKEN", ""),
    }


# ==========================================================
# Markdown Report Builder
# ==========================================================

def _build_markdown_report(evaluated_doc: dict, env: dict) -> str:
    """
    Build the high-level Markdown report for the PR comment
    and GitHub Actions Job Summary.

    Shows one row per tool with finding count only — no per-finding
    detail. This keeps the report fast to read and scannable.

    Args:
        evaluated_doc: Parsed evaluated.json dict.
        env:           Workflow environment variables.

    Returns:
        Complete Markdown report string.
    """
    overall_block        = bool(evaluated_doc.get("overall_block", True))
    blocking_results     = evaluated_doc.get("blocking_results", [])
    non_blocking_results = evaluated_doc.get("non_blocking_results", [])
    all_results          = blocking_results + non_blocking_results

    sha_short  = env["sha"][:7] if env["sha"] else "unknown"
    run_url    = f"{env['server_url']}/{env['repository']}/actions/runs/{env['run_id']}"
    commit_url = f"{env['server_url']}/{env['repository']}/commit/{env['sha']}"
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    lines: list[str] = []

    # Title and status banner
    lines += [
        "# CareConnect Quality Gate Report",
        "",
        "> **🚫 BLOCKED** — One or more required checks failed. "
        "Fix the issues below before merging."
        if overall_block else
        "> **✅ APPROVED** — All required checks passed.",
        "",
        PR_COMMENT_MARKER,
        "",
    ]

    # Report Header
    lines += [
        "### 📋 Report Header",
        "",
        "| Field | Value |",
        "|-------|-------|",
        f"| **Generated (UTC)** | {generated_at} |",
        f"| **Pipeline Run** | [#{env['run_number']}]({run_url}) |",
        f"| **Trigger** | `{env['event_name']}` |",
        f"| **Scan Root** | `{env['scan_root']}` |",
        "",
        "_All timestamps are reported in Coordinated Universal Time (UTC)._",
        "",
    ]

    # PR section — pull_request events only
    if env["event_name"] == "pull_request" and env["pr_number"]:
        lines += [
            "### 🔀 Pull Request",
            "",
            "| Field | Value |",
            "|-------|-------|",
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
        "| Field | Value |",
        "|-------|-------|",
        f"| **Commit SHA** | `{sha_short}` ([full]({commit_url})) |",
        "",
    ]

    # Tool Results Summary table
    lines += [
        "### 🛡️ Tool Results Summary",
        "",
        "| Tool | Category | Status | Blocking | Findings |",
        "|------|----------|--------|----------|----------|",
    ]

    for r in all_results:
        tool           = r.get("tool", "unknown")
        category       = CATEGORY_MAP.get(tool, "Analysis")
        violation      = r.get("policy_violation", False)
        blocking       = r.get("blocking", False)
        reason         = r.get("reason", "")
        normalized     = r.get("normalized", {})
        finding_count  = normalized.get("violation_count", 0)
        findings_label = f"{finding_count} finding(s)" if finding_count else "—"

        if reason == "disabled":
            status = "⏸️ DISABLED"
        elif violation:
            status = "❌ FAILURE"
        else:
            status = "✅ SUCCESS"

        blocking_label = "🚫 Yes" if blocking else "⚠️ Advisory"

        lines.append(
            f"| {tool} | {category} | {status} | {blocking_label} | {findings_label} |"
        )

    lines += [
        "",
        "---",
        "_Full artifact bundle available in the workflow run artifacts._",
        "",
    ]

    return "\n".join(lines)


# ==========================================================
# HTML Report Builder
# ==========================================================

def _severity_badge(severity: str | None) -> str:
    """
    Render an inline HTML severity badge with the appropriate color.

    Args:
        severity: Normalized severity string or None.

    Returns:
        HTML span element styled as a colored badge.
    """
    if not severity:
        return "<span>—</span>"
    color = SEVERITY_COLORS.get(severity.lower(), "#95a5a6")
    return (
        f'<span style="background:{color};color:#fff;padding:2px 8px;'
        f'border-radius:4px;font-size:0.85em;font-weight:bold;">'
        f'{severity.upper()}</span>'
    )


def _html_finding_row(finding: dict) -> str:
    """
    Render a single finding as an HTML table row.

    Args:
        finding: A normalized finding dict from findings[].

    Returns:
        HTML <tr> string.
    """
    severity = finding.get("severity", "")
    message  = finding.get("message", "—").replace("<", "&lt;").replace(">", "&gt;")
    file_    = finding.get("file", "—")
    line     = finding.get("line", "—")
    rule     = finding.get("rule", "—")
    rule_url = finding.get("rule_url", "")

    rule_cell = (
        f'<a href="{rule_url}" target="_blank">{rule}</a>'
        if rule_url else rule
    )

    return (
        f"<tr>"
        f"<td>{_severity_badge(severity)}</td>"
        f"<td><code>{file_}</code></td>"
        f"<td>{line}</td>"
        f"<td>{rule_cell}</td>"
        f"<td>{message}</td>"
        f"</tr>"
    )


def _html_tool_section(r: dict, section_label: str) -> str:
    """
    Render a full tool section for the HTML report.

    Includes enforcement status, severity counts, and a findings
    table with all per-finding detail.

    Args:
        r:             An evaluated result record (blocking or non-blocking).
        section_label: "Blocking" or "Non-Blocking" for display context.

    Returns:
        HTML string for this tool's section.
    """
    tool       = r.get("tool", "unknown")
    category   = CATEGORY_MAP.get(tool, "Analysis")
    violation  = r.get("policy_violation", False)
    blocking   = r.get("blocking", False)
    reason     = r.get("reason", "")
    normalized = r.get("normalized", {})
    findings   = normalized.get("findings", [])
    sev_counts = normalized.get("severity_counts", {})
    max_sev    = normalized.get("max_severity")
    executed   = normalized.get("executed", False)
    runtime_err = normalized.get("runtime_error", False)

    # Status and header color
    if reason == "disabled":
        status_html  = '<span style="color:#7f8c8d;">⏸️ DISABLED</span>'
        header_color = "#7f8c8d"
    elif violation:
        status_html  = '<span style="color:#c0392b;">❌ FAILURE</span>'
        header_color = "#c0392b"
    else:
        status_html  = '<span style="color:#27ae60;">✅ SUCCESS</span>'
        header_color = "#27ae60"

    blocking_html = (
        '<span style="color:#c0392b;">🚫 Yes</span>'
        if blocking else
        '<span style="color:#e67e22;">⚠️ Advisory</span>'
    )

    # Severity count pills
    sev_pills = ""
    for level in ["critical", "high", "medium", "low", "info"]:
        count = sev_counts.get(level, 0)
        if count:
            color = SEVERITY_COLORS.get(level, "#95a5a6")
            sev_pills += (
                f'<span style="background:{color};color:#fff;padding:2px 8px;'
                f'border-radius:4px;font-size:0.8em;margin-right:4px;">'
                f'{level.upper()}: {count}</span>'
            )
    if not sev_pills:
        sev_pills = '<span style="color:#7f8c8d;">No findings</span>'

    # Findings table
    if findings:
        rows = "\n".join(_html_finding_row(f) for f in findings)
        findings_html = f"""
        <table>
            <thead>
                <tr>
                    <th>Severity</th>
                    <th>File</th>
                    <th>Line</th>
                    <th>Rule</th>
                    <th>Message</th>
                </tr>
            </thead>
            <tbody>
                {rows}
            </tbody>
        </table>
        """
    elif reason == "disabled":
        findings_html = "<p><em>Tool is disabled. See parsers/sonar.py for activation steps.</em></p>"
    elif not executed:
        findings_html = "<p><em>Tool did not execute.</em></p>"
    elif runtime_err:
        err_msg = (normalized.get("metadata") or {}).get("error", "Unknown error")
        findings_html = f"<p><em>Runtime error: {err_msg}</em></p>"
    else:
        findings_html = "<p><em>No findings detected.</em></p>"

    return f"""
    <div class="tool-section">
        <div class="tool-header" style="border-left: 4px solid {header_color};">
            <div class="tool-title">
                <span class="tool-name">{tool}</span>
                <span class="tool-category">{category}</span>
            </div>
            <div class="tool-meta">
                <span>{status_html}</span>
                <span style="margin-left:12px;">Blocking: {blocking_html}</span>
                {f'<span style="margin-left:12px;color:#7f8c8d;font-size:0.85em;">Reason: <code>{reason}</code></span>' if reason and reason != "disabled" else ""}
            </div>
            <div class="sev-counts">{sev_pills}</div>
        </div>
        <div class="tool-findings">
            {findings_html}
        </div>
    </div>
    """


def _build_html_report(evaluated_doc: dict, normalized_doc: dict, env: dict) -> str:
    """
    Build the rich self-contained HTML report.

    Sections:
        1. Header and summary table (mirrors report.md)
        2. Blocking Tools — full per-finding detail
        3. Non-Blocking Tools — full per-finding detail

    Args:
        evaluated_doc:  Parsed evaluated.json dict.
        normalized_doc: Parsed normalized.json dict.
        env:            Workflow environment variables.

    Returns:
        Complete self-contained HTML string.
    """
    overall_block        = bool(evaluated_doc.get("overall_block", True))
    blocking_results     = evaluated_doc.get("blocking_results", [])
    non_blocking_results = evaluated_doc.get("non_blocking_results", [])

    sha_short    = env["sha"][:7] if env["sha"] else "unknown"
    run_url      = f"{env['server_url']}/{env['repository']}/actions/runs/{env['run_id']}"
    commit_url   = f"{env['server_url']}/{env['repository']}/commit/{env['sha']}"
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")

    banner_color = "#c0392b" if overall_block else "#27ae60"
    banner_text  = (
        "🚫 BLOCKED — One or more required checks failed. Fix the issues below before merging."
        if overall_block else
        "✅ APPROVED — All required checks passed."
    )

    # ----------------------------------------------------------
    # Summary table rows
    # ----------------------------------------------------------
    all_results   = blocking_results + non_blocking_results
    summary_rows  = ""
    for r in all_results:
        tool          = r.get("tool", "unknown")
        category      = CATEGORY_MAP.get(tool, "Analysis")
        violation     = r.get("policy_violation", False)
        blocking      = r.get("blocking", False)
        reason        = r.get("reason", "")
        normalized    = r.get("normalized", {})
        finding_count = normalized.get("violation_count", 0)

        if reason == "disabled":
            status_cell = '<span style="color:#7f8c8d;">⏸️ DISABLED</span>'
        elif violation:
            status_cell = '<span style="color:#c0392b;">❌ FAILURE</span>'
        else:
            status_cell = '<span style="color:#27ae60;">✅ SUCCESS</span>'

        blocking_cell = (
            '<span style="color:#c0392b;">🚫 Yes</span>'
            if blocking else
            '<span style="color:#e67e22;">⚠️ Advisory</span>'
        )
        findings_cell = str(finding_count) if finding_count else "—"

        summary_rows += (
            f"<tr><td><code>{tool}</code></td><td>{category}</td>"
            f"<td>{status_cell}</td><td>{blocking_cell}</td>"
            f"<td>{findings_cell}</td></tr>\n"
        )

    # ----------------------------------------------------------
    # PR info block
    # ----------------------------------------------------------
    pr_block = ""
    if env["event_name"] == "pull_request" and env["pr_number"]:
        pr_block = f"""
        <div class="info-card">
            <h3>🔀 Pull Request</h3>
            <table class="info-table">
                <tr><td><strong>PR Number</strong></td><td>#{env['pr_number']}</td></tr>
                <tr><td><strong>PR Author</strong></td><td>@{env['actor']}</td></tr>
                <tr><td><strong>Source Branch</strong></td><td><code>{env['head_ref']}</code></td></tr>
                <tr><td><strong>Target Branch</strong></td><td><code>{env['base_ref']}</code></td></tr>
            </table>
        </div>
        """

    # ----------------------------------------------------------
    # Blocking and non-blocking tool sections
    # ----------------------------------------------------------
    blocking_sections = "\n".join(
        _html_tool_section(r, "Blocking") for r in blocking_results
    ) or "<p><em>No blocking tools configured.</em></p>"

    non_blocking_sections = "\n".join(
        _html_tool_section(r, "Non-Blocking") for r in non_blocking_results
    ) or "<p><em>No advisory tools configured.</em></p>"

    # ----------------------------------------------------------
    # Inline CSS
    # ----------------------------------------------------------
    css = """
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: #f5f6fa;
            color: #2c3e50;
            padding: 24px;
            font-size: 14px;
        }
        h1 { font-size: 1.6em; margin-bottom: 8px; }
        h2 { font-size: 1.2em; margin: 24px 0 12px; padding-bottom: 6px;
             border-bottom: 2px solid #dde; }
        h3 { font-size: 1em; margin-bottom: 8px; color: #555; }
        .banner {
            padding: 12px 20px;
            border-radius: 6px;
            color: #fff;
            font-weight: bold;
            font-size: 1.05em;
            margin: 16px 0;
        }
        .info-card {
            background: #fff;
            border-radius: 6px;
            padding: 16px 20px;
            margin-bottom: 16px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
        }
        .info-table td { padding: 4px 12px 4px 0; vertical-align: top; }
        .info-table td:first-child { color: #7f8c8d; white-space: nowrap; }
        table {
            width: 100%;
            border-collapse: collapse;
            background: #fff;
            border-radius: 6px;
            overflow: hidden;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
            margin-bottom: 16px;
        }
        th {
            background: #2c3e50;
            color: #fff;
            padding: 10px 14px;
            text-align: left;
            font-size: 0.85em;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }
        td { padding: 8px 14px; border-bottom: 1px solid #eee; vertical-align: top; }
        tr:last-child td { border-bottom: none; }
        tr:hover td { background: #f8f9fa; }
        code {
            background: #f0f2f5;
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 0.9em;
            font-family: "SFMono-Regular", Consolas, monospace;
        }
        .tool-section {
            background: #fff;
            border-radius: 6px;
            margin-bottom: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
            overflow: hidden;
        }
        .tool-header {
            padding: 14px 20px;
            background: #fafbfc;
            border-bottom: 1px solid #eee;
        }
        .tool-title {
            display: flex;
            align-items: center;
            gap: 12px;
            margin-bottom: 6px;
        }
        .tool-name {
            font-weight: bold;
            font-size: 1.05em;
            font-family: "SFMono-Regular", Consolas, monospace;
        }
        .tool-category {
            color: #7f8c8d;
            font-size: 0.85em;
        }
        .tool-meta {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            font-size: 0.9em;
        }
        .sev-counts { margin-top: 6px; }
        .tool-findings { padding: 16px 20px; }
        .section-header {
            background: #2c3e50;
            color: #fff;
            padding: 10px 20px;
            border-radius: 6px;
            margin: 24px 0 12px;
            font-weight: bold;
            font-size: 1.05em;
        }
        .section-header.advisory { background: #7f8c8d; }
        footer {
            margin-top: 32px;
            padding-top: 12px;
            border-top: 1px solid #dde;
            color: #7f8c8d;
            font-size: 0.85em;
        }
        a { color: #2980b9; text-decoration: none; }
        a:hover { text-decoration: underline; }
    """

    # ----------------------------------------------------------
    # Assemble full HTML document
    # ----------------------------------------------------------
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CareConnect Quality Gate Report</title>
    <style>{css}</style>
</head>
<body>

    <h1>CareConnect Quality Gate Report</h1>
    <div class="banner" style="background:{banner_color};">{banner_text}</div>

    <div class="info-card">
        <h3>📋 Report Header</h3>
        <table class="info-table">
            <tr><td><strong>Generated (UTC)</strong></td><td>{generated_at}</td></tr>
            <tr><td><strong>Pipeline Run</strong></td>
                <td><a href="{run_url}" target="_blank">#{env['run_number']}</a></td></tr>
            <tr><td><strong>Trigger</strong></td><td><code>{env['event_name']}</code></td></tr>
            <tr><td><strong>Scan Root</strong></td><td><code>{env['scan_root']}</code></td></tr>
            <tr><td><strong>Commit SHA</strong></td>
                <td><a href="{commit_url}" target="_blank"><code>{sha_short}</code></a></td></tr>
        </table>
    </div>

    {pr_block}

    <h2>🛡️ Tool Results Summary</h2>
    <table>
        <thead>
            <tr>
                <th>Tool</th>
                <th>Category</th>
                <th>Status</th>
                <th>Blocking</th>
                <th>Findings</th>
            </tr>
        </thead>
        <tbody>
            {summary_rows}
        </tbody>
    </table>

    <div class="section-header">🚫 Blocking Tools</div>
    {blocking_sections}

    <div class="section-header advisory">⚠️ Advisory Tools (Non-Blocking)</div>
    {non_blocking_sections}

    <footer>
        Generated by CareConnect Quality Gate Engine &mdash; {generated_at}
    </footer>

</body>
</html>"""


# ==========================================================
# PR Comment
# ==========================================================

def _post_or_update_pr_comment(body: str, env: dict) -> None:
    """
    Post or update a single PR comment with the Markdown report.

    Finds an existing bot comment containing PR_COMMENT_MARKER
    and updates it in place. Creates a new comment if none exists.
    This prevents comment spam on repeated runs.

    Args:
        body: The full Markdown report string.
        env:  Workflow environment variables dict.
    """
    token      = env["token"]
    repository = env["repository"]
    pr_number  = env["pr_number"]

    if not all([token, repository, pr_number]):
        print("[report] Skipping PR comment — missing GITHUB_TOKEN, "
              "GITHUB_REPOSITORY, or PR_NUMBER.")
        return

    api_base = "https://api.github.com"
    headers  = {
        "Authorization":        f"Bearer {token}",
        "Accept":               "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }

    # Search for existing bot comment across all pages
    existing_comment_id = None
    page = 1

    while True:
        resp = requests.get(
            f"{api_base}/repos/{repository}/issues/{pr_number}/comments",
            headers=headers,
            params={"per_page": 100, "page": page},
            timeout=15,
        )
        if resp.status_code != 200:
            print(f"[report] Warning: could not list PR comments "
                  f"(HTTP {resp.status_code}).")
            break

        comments = resp.json()
        if not comments:
            break

        for comment in comments:
            login = (comment.get("user") or {}).get("login", "")
            if login == "github-actions[bot]" and \
                    PR_COMMENT_MARKER in (comment.get("body") or ""):
                existing_comment_id = comment["id"]
                break

        if existing_comment_id or len(comments) < 100:
            break
        page += 1

    # Update existing or create new comment
    if existing_comment_id:
        resp = requests.patch(
            f"{api_base}/repos/{repository}/issues/comments/{existing_comment_id}",
            headers=headers,
            json={"body": body},
            timeout=15,
        )
        if resp.status_code == 200:
            print(f"[report] PR comment updated (id={existing_comment_id}).")
        else:
            print(f"[report] Warning: failed to update PR comment "
                  f"(HTTP {resp.status_code}).")
    else:
        resp = requests.post(
            f"{api_base}/repos/{repository}/issues/{pr_number}/comments",
            headers=headers,
            json={"body": body},
            timeout=15,
        )
        if resp.status_code == 201:
            print("[report] PR comment created.")
        else:
            print(f"[report] Warning: failed to create PR comment "
                  f"(HTTP {resp.status_code}).")


# ==========================================================
# Main Entry Point
# ==========================================================

def generate_report() -> None:
    """
    Generate report.md and report.html, then post the PR comment.

    Contract:
        - Always writes report.md (even on partial failures).
        - Always writes report.html (even on partial failures).
        - PR comment posted/updated on pull_request events only.
        - Failures in PR comment posting do not raise exceptions.
    """
    env = _get_env()

    # ----------------------------------------------------------
    # Load evaluated.json
    # ----------------------------------------------------------
    if not EVALUATED_FILE.exists():
        print(f"[report] ❌ evaluated.json not found at {EVALUATED_FILE}.")
        sys.exit(1)

    try:
        with open(EVALUATED_FILE, "r", encoding="utf-8") as f:
            evaluated_doc = json.load(f)
    except Exception as e:
        print(f"[report] ❌ Failed to parse evaluated.json: {e}")
        sys.exit(1)

    # ----------------------------------------------------------
    # Load normalized.json (for HTML report findings detail)
    # ----------------------------------------------------------
    normalized_doc: dict = {}
    try:
        if NORMALIZED_FILE.exists():
            with open(NORMALIZED_FILE, "r", encoding="utf-8") as f:
                normalized_doc = json.load(f)
    except Exception as e:
        print(f"[report] Warning: could not load normalized.json: {e}")

    # ----------------------------------------------------------
    # Write report.md
    # ----------------------------------------------------------
    REPORT_MD_FILE.parent.mkdir(parents=True, exist_ok=True)
    md_body = _build_markdown_report(evaluated_doc, env)
    REPORT_MD_FILE.write_text(md_body, encoding="utf-8")
    print(f"[report] report.md written to: {REPORT_MD_FILE}")

    # ----------------------------------------------------------
    # Write report.html
    # ----------------------------------------------------------
    html_body = _build_html_report(evaluated_doc, normalized_doc, env)
    REPORT_HTML_FILE.write_text(html_body, encoding="utf-8")
    print(f"[report] report.html written to: {REPORT_HTML_FILE}")

    # ----------------------------------------------------------
    # Post PR comment (pull_request events only)
    # ----------------------------------------------------------
    if env["event_name"] == "pull_request" and env["pr_number"]:
        _post_or_update_pr_comment(md_body, env)
    else:
        print("[report] Not a PR event — skipping PR comment.")


if __name__ == "__main__":
    generate_report()
    