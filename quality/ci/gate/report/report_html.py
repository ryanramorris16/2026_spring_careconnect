# File: quality/ci/gate/reports/report_html.py
# ==========================================================
# HTML Report Builder
# ----------------------------------------------------------
# Builds the complete HTML report string from evaluated.json
# data.
#
# Functions:
#   build_html_report(evaluated_doc, env) → str
# ==========================================================

from datetime import datetime, timezone

from quality.ci.gate.report.report_constants import CATEGORY_MAP, SEVERITY_COLORS


CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    background: #f5f6fa; color: #2c3e50; padding: 24px; font-size: 14px;
}
h1 { font-size: 1.6em; margin-bottom: 8px; }
h2 { font-size: 1.2em; margin: 24px 0 12px; padding-bottom: 6px;
     border-bottom: 2px solid #dde; }
h3 { font-size: 1em; margin-bottom: 8px; color: #555; }
.banner { padding: 12px 20px; border-radius: 6px; color: #fff;
          font-weight: bold; font-size: 1.05em; margin: 16px 0; }
.info-card { background: #fff; border-radius: 6px; padding: 16px 20px;
             margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.info-table td { padding: 4px 12px 4px 0; vertical-align: top; }
.info-table td:first-child { color: #7f8c8d; white-space: nowrap; }
table { width: 100%; border-collapse: collapse; background: #fff;
        border-radius: 6px; overflow: hidden;
        box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
th { background: #2c3e50; color: #fff; padding: 10px 14px; text-align: left;
     font-size: 0.85em; text-transform: uppercase; letter-spacing: 0.05em; }
td { padding: 8px 14px; border-bottom: 1px solid #eee; vertical-align: top; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: #f8f9fa; }
code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px;
       font-size: 0.9em; font-family: "SFMono-Regular", Consolas, monospace; }
.tool-section { background: #fff; border-radius: 6px; margin-bottom: 20px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.08); overflow: hidden; }
.tool-header { padding: 14px 20px; background: #fafbfc;
               border-bottom: 1px solid #eee; }
.tool-title { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.tool-name { font-weight: bold; font-size: 1.05em;
             font-family: "SFMono-Regular", Consolas, monospace; }
.tool-category { color: #7f8c8d; font-size: 0.85em; }
.tool-meta { display: flex; align-items: center;
             margin-bottom: 8px; font-size: 0.9em; }
.sev-counts { margin-top: 6px; }
.tool-findings { padding: 16px 20px; }
.section-header { background: #2c3e50; color: #fff; padding: 10px 20px;
                  border-radius: 6px; margin: 24px 0 12px;
                  font-weight: bold; font-size: 1.05em; }
.section-header.advisory { background: #7f8c8d; }
footer { margin-top: 32px; padding-top: 12px;
         border-top: 1px solid #dde; color: #7f8c8d; font-size: 0.85em; }
a { color: #2980b9; text-decoration: none; }
a:hover { text-decoration: underline; }
a.tool-link { color: #2980b9; text-decoration: none;
              font-family: "SFMono-Regular", Consolas, monospace; }
a.tool-link:hover { text-decoration: underline; }
"""


# ----------------------------------------------------------
# Component builders
# ----------------------------------------------------------

def _severity_badge(severity: str | None) -> str:
    if not severity:
        return "<span>—</span>"
    color = SEVERITY_COLORS.get(severity.lower(), "#95a5a6")
    return (
        f'<span style="background:{color};color:#fff;padding:2px 8px;'
        f'border-radius:4px;font-size:0.85em;font-weight:bold;">'
        f'{severity.upper()}</span>'
    )


def _finding_row(finding: dict) -> str:
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


def _sev_pills(sev_counts: dict) -> str:
    pills = ""
    for level in ["critical", "high", "medium", "low", "info"]:
        count = sev_counts.get(level, 0)
        if count:
            color  = SEVERITY_COLORS.get(level, "#95a5a6")
            pills += (
                f'<span style="background:{color};color:#fff;padding:2px 8px;'
                f'border-radius:4px;font-size:0.8em;margin-right:4px;">'
                f'{level.upper()}: {count}</span>'
            )
    return pills or '<span style="color:#7f8c8d;">No findings</span>'


def _tool_section(r: dict) -> str:
    tool        = r.get("tool", "unknown")
    category    = CATEGORY_MAP.get(tool, "Analysis")
    violation   = r.get("policy_violation", False)
    blocking    = r.get("blocking", False)
    reason      = r.get("reason", "")
    normalized  = r.get("normalized", {})
    findings    = normalized.get("findings", [])
    sev_counts  = normalized.get("severity_counts", {})
    executed    = normalized.get("executed", False)
    runtime_err = normalized.get("runtime_error", False)

    if reason == "disabled":
        status_html  = '<span style="color:#7f8c8d;">⏸️ DISABLED</span>'
        header_color = "#7f8c8d"
    elif violation:
        status_html  = '<span style="color:#c0392b;">❌ FAILURE</span>'
        header_color = "#c0392b"
    else:
        status_html  = '<span style="color:#27ae60;">✅ SUCCESS</span>'
        header_color = "#27ae60"

    role_html = (
        '<span style="color:#c0392b;">🚫 Enforced</span>'
        if blocking else
        '<span style="color:#e67e22;">⚠️ Advisory</span>'
    )

    reason_html = (
        f'<span style="margin-left:12px;color:#7f8c8d;font-size:0.85em;">'
        f'Reason: <code>{reason}</code></span>'
        if reason and reason != "disabled" else ""
    )

    if findings:
        rows          = "\n".join(_finding_row(f) for f in findings)
        findings_html = f"""
        <table>
            <thead><tr>
                <th>Severity</th><th>File</th><th>Line</th>
                <th>Rule</th><th>Message</th>
            </tr></thead>
            <tbody>{rows}</tbody>
        </table>"""
    elif reason == "disabled":
        findings_html = "<p><em>Tool is disabled.</em></p>"
    elif not executed:
        findings_html = "<p><em>Tool did not execute.</em></p>"
    elif runtime_err:
        err_msg       = (normalized.get("metadata") or {}).get("error", "Unknown error")
        findings_html = f"<p><em>Runtime error: {err_msg}</em></p>"
    else:
        findings_html = "<p><em>No findings detected.</em></p>"

    return f"""
    <div class="tool-section" id="tool-{tool}">
        <div class="tool-header" style="border-left:4px solid {header_color};">
            <div class="tool-title">
                <span class="tool-name">{tool}</span>
                <span class="tool-category">{category}</span>
            </div>
            <div class="tool-meta">
                {status_html}
                <span style="margin-left:12px;">Role: {role_html}</span>
                {reason_html}
            </div>
            <div class="sev-counts">{_sev_pills(sev_counts)}</div>
        </div>
        <div class="tool-findings">{findings_html}</div>
    </div>"""


def _summary_row(r: dict) -> str:
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

    role_cell = (
        '<span style="color:#c0392b;">🚫 Enforced</span>'
        if blocking else
        '<span style="color:#e67e22;">⚠️ Advisory</span>'
    )
    findings_cell = str(finding_count) if finding_count else "—"

    # Tool name links to its detail section below
    return (
        f"<tr>"
        f'<td><a class="tool-link" href="#tool-{tool}">'
        f'<code>{tool}</code></a></td>'
        f"<td>{category}</td>"
        f"<td>{status_cell}</td>"
        f"<td>{role_cell}</td>"
        f"<td>{findings_cell}</td>"
        f"</tr>\n"
    )


def _pr_block(env: dict) -> str:
    if env["event_name"] != "pull_request" or not env["pr_number"]:
        return ""
    return f"""
    <div class="info-card">
        <h3>🔀 Pull Request</h3>
        <table class="info-table">
            <tr><td><strong>PR Number</strong></td><td>#{env['pr_number']}</td></tr>
            <tr><td><strong>PR Author</strong></td><td>@{env['actor']}</td></tr>
            <tr><td><strong>Source Branch</strong></td>
                <td><code>{env['head_ref']}</code></td></tr>
            <tr><td><strong>Target Branch</strong></td>
                <td><code>{env['base_ref']}</code></td></tr>
        </table>
    </div>"""


LEGEND_BLOCK = """
    <div class="info-card">
        <h3>📖 Legend</h3>
        <table class="info-table">
            <tr><td>✅ SUCCESS</td>
                <td>Tool ran and found no violations</td></tr>
            <tr><td>❌ FAILURE</td>
                <td>Tool found one or more violations</td></tr>
            <tr><td>⏸️ DISABLED</td>
                <td>Tool is not yet configured</td></tr>
            <tr><td>🚫 Enforced</td>
                <td>Violations from this tool will block the merge</td></tr>
            <tr><td>⚠️ Advisory</td>
                <td>Violations are reported but will not block the merge</td></tr>
        </table>
    </div>"""


# ----------------------------------------------------------
# Main builder
# ----------------------------------------------------------

def build_html_report(evaluated_doc: dict, env: dict) -> str:

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

    banner_color = "#c0392b" if overall_block else "#27ae60"
    banner_text  = (
        "🚫 BLOCKED — One or more required checks failed. "
        "Fix the issues below before merging."
        if overall_block else
        "✅ APPROVED — All required checks passed."
    )

    summary_rows      = "".join(_summary_row(r) for r in all_results)
    blocking_sections = (
        "\n".join(_tool_section(r) for r in blocking_results)
        or "<p><em>No enforced tools configured.</em></p>"
    )
    non_blocking_sections = (
        "\n".join(_tool_section(r) for r in non_blocking_results)
        or "<p><em>No advisory tools configured.</em></p>"
    )

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CareConnect Quality Gate Report</title>
    <style>{CSS}</style>
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
        <tr><td><strong>Trigger</strong></td>
            <td><code>{env['event_name']}</code></td></tr>
        <tr><td><strong>Scan Root</strong></td>
            <td><code>{env['scan_root']}</code></td></tr>
        <tr><td><strong>Commit SHA</strong></td>
            <td><a href="{commit_url}" target="_blank">
                <code>{sha_short}</code></a></td></tr>
    </table>
</div>

{_pr_block(env)}

{LEGEND_BLOCK}

<h2>🛡️ Tool Results Summary</h2>
<table>
    <thead>
        <tr>
            <th>Tool</th><th>Category</th><th>Status</th>
            <th>Role</th><th>Findings</th>
        </tr>
    </thead>
    <tbody>{summary_rows}</tbody>
</table>

<div class="section-header">🚫 Enforced Tools</div>
{blocking_sections}

<div class="section-header advisory">⚠️ Advisory Tools</div>
{non_blocking_sections}

<footer>
    Generated by CareConnect Quality Gate Engine &mdash; {generated_at}
</footer>

</body>
</html>"""
