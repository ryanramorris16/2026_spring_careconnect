# File: quality/local/report/report_html.py
# ==========================================================
# Report HTML Builder
# ----------------------------------------------------------
# Builds the complete HTML report string from parsed findings.
# ==========================================================

SEVERITY_COLORS = {
	"critical": "#7c0000",
	"high": "#c0392b",
	"medium": "#e67e22",
	"low": "#f1c40f",
	"info": "#3498db",
}

CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: #f5f6fa; color: #2c3e50; padding: 24px; font-size: 14px;
}
h1 { font-size: 1.6em; margin-bottom: 8px; }
h2 { font-size: 1.2em; margin: 24px 0 12px; padding-bottom: 6px; border-bottom: 2px solid #dde; }
h3 { font-size: 1em; margin-bottom: 8px; color: #555; }
.banner { padding: 12px 20px; border-radius: 6px; color: #fff; font-weight: bold; font-size: 1.05em; margin: 16px 0; }
.info-card { background: #fff; border-radius: 6px; padding: 16px 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.info-table td { padding: 4px 12px 4px 0; vertical-align: top; }
.info-table td:first-child { color: #7f8c8d; white-space: nowrap; }
table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 6px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 16px; }
th { background: #2c3e50; color: #fff; padding: 10px 14px; text-align: left; font-size: 0.85em; text-transform: uppercase; letter-spacing: 0.05em; }
td { padding: 8px 14px; border-bottom: 1px solid #eee; vertical-align: top; }
tr:last-child td { border-bottom: none; }
tr:hover td { background: #f8f9fa; }
code { background: #f0f2f5; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; font-family: "SFMono-Regular", Consolas, monospace; }
.tool-section { background: #fff; border-radius: 6px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); overflow: hidden; }
.tool-header { padding: 14px 20px; background: #fafbfc; border-bottom: 1px solid #eee; }
.tool-title { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.tool-name { font-weight: bold; font-size: 1.05em; font-family: "SFMono-Regular", Consolas, monospace; }
.tool-category { color: #7f8c8d; font-size: 0.85em; }
.tool-meta { display: flex; align-items: center; margin-bottom: 8px; font-size: 0.9em; }
.sev-counts { margin-top: 6px; }
.tool-findings { padding: 16px 20px; }
.section-header { background: #2c3e50; color: #fff; padding: 10px 20px; border-radius: 6px; margin: 24px 0 12px; font-weight: bold; font-size: 1.05em; }
footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid #dde; color: #7f8c8d; font-size: 0.85em; }
a.tool-link { color: #2980b9; text-decoration: none; font-family: "SFMono-Regular", Consolas, monospace; }
a.tool-link:hover { text-decoration: underline; }
a { color: #2980b9; text-decoration: none; }
a:hover { text-decoration: underline; }
"""


def _severity_badge(sev: str) -> str:
	sev = (sev or "info").lower()
	color = SEVERITY_COLORS.get(sev, "#95a5a6")
	return (
		f'<span style="background:{color};color:#fff;padding:2px 8px;'
		f'border-radius:4px;font-size:0.85em;font-weight:bold;">'
		f"{sev.upper()}</span>"
	)


def _status_html(status: str) -> str:
	if status == "passed":
		return '<span style="color:#27ae60;">&#x2705; PASSED</span>'
	if status == "failed":
		return '<span style="color:#c0392b;">&#x274C; FAILED</span>'
	return '<span style="color:#7f8c8d;">&#x23F8;&#xFE0F; SKIPPED</span>'


def _border_color(status: str) -> str:
	if status == "passed":
		return "#27ae60"
	if status == "failed":
		return "#c0392b"
	return "#7f8c8d"


def _sev_pills(counts: dict) -> str:
	pills = ""
	for level in ["critical", "high", "medium", "low", "info"]:
		count = counts.get(level, 0)
		if count:
			color = SEVERITY_COLORS.get(level, "#95a5a6")
			pills += (
				f'<span style="background:{color};color:#fff;padding:2px 8px;'
				f'border-radius:4px;font-size:0.8em;margin-right:4px;">'
				f"{level.upper()}: {count}</span>"
			)
	return pills or '<span style="color:#7f8c8d;">No findings</span>'


def _finding_rows(findings: list) -> str:
	if not findings:
		return "<p><em>No findings detected.</em></p>"
	rows = ""
	for finding in findings:
		msg = (finding.get("message") or "").replace("<", "&lt;").replace(">", "&gt;")
		rows += (
			f"<tr>"
			f"<td>{_severity_badge(finding.get('severity', 'info'))}</td>"
			f"<td><code>{finding.get('file', 'unknown')}</code></td>"
			f"<td>{finding.get('line', '0')}</td>"
			f"<td>{finding.get('rule', 'unknown')}</td>"
			f"<td>{msg}</td>"
			f"</tr>"
		)
	return (
		"<table><thead><tr>"
		"<th>Severity</th><th>File</th><th>Line</th><th>Rule</th><th>Message</th>"
		f"</tr></thead><tbody>{rows}</tbody></table>"
	)


def _tool_section(tool: str, category: str, status: str, counts: dict, findings: list) -> str:
	border_color = _border_color(status)
	total = sum(counts.values())
	pills = f'<div class="sev-counts">{_sev_pills(counts)}</div>' if total > 0 else ""
	return f"""
<div class="tool-section" id="tool-{tool}">
  <div class="tool-header" style="border-left:4px solid {border_color};">
	<div class="tool-title">
	  <span class="tool-name">{tool}</span>
	  <span class="tool-category">{category}</span>
	</div>
	<div class="tool-meta">
	  {_status_html(status)}
	  <span style="margin-left:12px;">&#x1F6AB; Enforced</span>
	</div>
	{pills}
  </div>
  <div class="tool-findings">{_finding_rows(findings)}</div>
</div>"""


def _summary_row(tool: str, category: str, status: str, count: int) -> str:
	display_count = str(count) if count > 0 else "&#x2014;"
	return (
		"<tr>"
		f"<td><a class=\"tool-link\" href=\"#tool-{tool}\"><code>{tool}</code></a></td>"
		f"<td>{category}</td>"
		f"<td>{_status_html(status)}</td>"
		"<td>&#x1F6AB; Enforced</td>"
		f"<td>{display_count}</td>"
		"</tr>"
	)


def build_html(context: dict) -> str:
	generated_at = context.get("generated_at", "")
	scan_user = context.get("scan_user", "")
	repo_root = context.get("repo_root", "")
	failed = int(context.get("failed", 0))

	fl_status = context.get("fl_status", "skipped")
	cs_status = context.get("cs_status", "skipped")
	pmd_status = context.get("pmd_status", "skipped")
	sb_status = context.get("sb_status", "skipped")

	fl_findings = context.get("fl_findings", [])
	cs_findings = context.get("cs_findings", [])
	pmd_findings = context.get("pmd_findings", [])
	sb_findings = context.get("sb_findings", [])

	zero = {"critical": 0, "high": 0, "medium": 0, "low": 0, "info": 0}
	fl_sev = context.get("fl_sev", zero)
	cs_sev = context.get("cs_sev", zero)
	pmd_sev = context.get("pmd_sev", zero)
	sb_sev = context.get("sb_sev", zero)

	if failed == 0:
		banner_color = "#27ae60"
		banner_text = "&#x2705; APPROVED &#x2014; All required checks passed."
	else:
		banner_color = "#c0392b"
		banner_text = f"&#x1F6AB; BLOCKED &#x2014; {failed} tool(s) failed. Fix the issues below before committing."

	return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CareConnect Local Quality Gate Report</title>
<style>{CSS}</style>
</head>
<body>
<h1>CareConnect Local Quality Gate Report</h1>
<div class="banner" style="background:{banner_color};">{banner_text}</div>
<div class="info-card">
<h3>&#x1F4CB; Report Header</h3>
<table class="info-table">
<tr><td><strong>Generated (UTC)</strong></td><td>{generated_at}</td></tr>
<tr><td><strong>Trigger</strong></td><td>local pre-commit</td></tr>
<tr><td><strong>User</strong></td><td>{scan_user}</td></tr>
<tr><td><strong>Scan Root</strong></td><td><code>{repo_root}</code></td></tr>
</table>
</div>
<div class="info-card">
<h3>&#x1F4D6; Legend</h3>
<table class="info-table">
<tr><td>&#x2705; PASSED</td><td>Tool ran and found no violations</td></tr>
<tr><td>&#x274C; FAILED</td><td>Tool found one or more violations</td></tr>
<tr><td>&#x23F8;&#xFE0F; SKIPPED</td><td>Tool not installed or project not found</td></tr>
<tr><td>&#x1F6AB; Enforced</td><td>Violations from this tool will block the commit</td></tr>
</table>
</div>
<h2>&#x1F6E1;&#xFE0F; Tool Results Summary</h2>
<table>
<thead>
<tr><th>Tool</th><th>Category</th><th>Status</th><th>Role</th><th>Findings</th></tr>
</thead>
<tbody>
{_summary_row("flutter_analyze", "SAST &#x2014; Flutter", fl_status, len(fl_findings))}
{_summary_row("checkstyle", "SAST &#x2014; Java", cs_status, len(cs_findings))}
{_summary_row("pmd", "SAST &#x2014; Java", pmd_status, len(pmd_findings))}
{_summary_row("spotbugs", "SAST &#x2014; Java", sb_status, len(sb_findings))}
</tbody>
</table>
<div class="section-header">&#x1F6AB; Enforced Tools</div>
{_tool_section("flutter_analyze", "SAST &#x2014; Flutter", fl_status, fl_sev, fl_findings)}
{_tool_section("checkstyle", "SAST &#x2014; Java", cs_status, cs_sev, cs_findings)}
{_tool_section("pmd", "SAST &#x2014; Java", pmd_status, pmd_sev, pmd_findings)}
{_tool_section("spotbugs", "SAST &#x2014; Java", sb_status, sb_sev, sb_findings)}
<footer>
Generated by CareConnect Local Quality Gate &#x2014; {generated_at}
</footer>
</body>
</html>"""