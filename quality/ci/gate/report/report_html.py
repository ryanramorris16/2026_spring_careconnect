"""
HTML Report Builder

Builds the complete HTML report string from evaluated.json data.

Functions
---------
build_html_report(evaluated_doc, env) -> str
    Build the full HTML quality gate report document.
"""

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
info-table td:first-child { color: #7f8c8d; white-space: nowrap; }
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
"""


def build_html_report(evaluated_doc: dict, env: dict) -> str:
    """
    Build the complete HTML report from evaluated results.
    """

    report_data = {
        "overall_block": bool(evaluated_doc.get("overall_block", True)),
        "blocking_results": evaluated_doc.get("blocking_results", []),
        "non_blocking_results": evaluated_doc.get("non_blocking_results", []),
    }

    report_data["all_results"] = (
        report_data["blocking_results"] + report_data["non_blocking_results"]
    )

    render_data = {
        "sha_short": env["sha"][:7] if env["sha"] else "unknown",
        "run_url": f"{env['server_url']}/{env['repository']}/actions/runs/{env['run_id']}",
        "commit_url": f"{env['server_url']}/{env['repository']}/commit/{env['sha']}",
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC"),
        "banner_color": "#c0392b" if report_data["overall_block"] else "#27ae60",
        "banner_text": (
            "BLOCKED — One or more required checks failed."
            if report_data["overall_block"]
            else "APPROVED — All required checks passed."
        ),
        "summary_rows": "".join(
            f"<tr><td>{r.get('tool')}</td></tr>" for r in report_data["all_results"]
        ),
        "blocking_sections": "\n".join(
            f"<p>{r.get('tool')}</p>" for r in report_data["blocking_results"]
        )
        or "<p><em>No enforced tools configured.</em></p>",
        "non_blocking_sections": "\n".join(
            f"<p>{r.get('tool')}</p>" for r in report_data["non_blocking_results"]
        )
        or "<p><em>No advisory tools configured.</em></p>",
    }

    return f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Quality Gate Report</title>
<style>{CSS}</style>
</head>

<body>

<h1>CareConnect Quality Gate Report</h1>
<div class="banner" style="background:{render_data["banner_color"]};">
{render_data["banner_text"]}
</div>

<div class="info-card">
<table class="info-table">
<tr><td><strong>Generated</strong></td>
<td>{render_data["generated_at"]}</td></tr>

<tr><td><strong>Pipeline Run</strong></td>
<td><a href="{render_data["run_url"]}" target="_blank">
#{env["run_number"]}
</a></td></tr>

<tr><td><strong>Commit</strong></td>
<td><a href="{render_data["commit_url"]}" target="_blank">
<code>{render_data["sha_short"]}</code>
</a></td></tr>

</table>
</div>

<h2>Tool Results Summary</h2>

<table>
<thead>
<tr>
<th>Tool</th>
</tr>
</thead>
<tbody>
{render_data["summary_rows"]}
</tbody>
</table>

<div class="section-header">Enforced Tools</div>
{render_data["blocking_sections"]}

<div class="section-header advisory">Advisory Tools</div>
{render_data["non_blocking_sections"]}

<footer>
Generated by CareConnect Quality Gate Engine — {render_data["generated_at"]}
</footer>

</body>
</html>
"""
