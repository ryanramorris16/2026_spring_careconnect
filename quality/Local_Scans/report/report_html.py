"""
Report HTML Builder

Builds the complete HTML report string from parsed findings.
Matches the CI report style from quality/ci/gate/report.py.

Functions:
  build_html(context) → str

context dict keys:
  generated_at  — UTC timestamp string
  scan_user     — local username
  repo_root     — repository root path
  failed        — number of failed tools
  fl_status     — passed | failed | skipped
  cs_status     — passed | failed | skipped
  pmd_status    — passed | failed | skipped
  sb_status     — passed | failed | skipped
  fl_findings   — list of finding dicts
  cs_findings   — list of finding dicts
  pmd_findings  — list of finding dicts
  sb_findings   — list of finding dicts
  fl_sev        — severity count dict
  cs_sev        — severity count dict
  pmd_sev       — severity count dict
  sb_sev        — severity count dict
"""

# ----------------------------------------------------------
# Constants
# ----------------------------------------------------------

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
footer { margin-top: 32px; padding-top: 12px;
         border-top: 1px solid #dde; color: #7f8c8d; font-size: 0.85em; }
a.tool-link { color: #2980b9; text-decoration: none;
              font-family: "SFMono-Regular", Consolas, monospace; }
a.tool-link:hover { text-decoration: underline; }
a { color: #2980b9; text-decoration: none; }
a:hover { text-decoration: underline; }
"""

# ----------------------------------------------------------
# Component builders
# ----------------------------------------------------------

def _severity_badge(sev: str) -> str:
    sev = (sev or "info").lower()
    color = SEVERITY_COLORS.get(sev, "#95a5a6")
    return (
        f'<span style="background:{color};color:#fff;padding:2px 8px;'
        f'border-radius:4px;font-size:0.85em;font-weight:bold;">'
        f'{sev.upper()}</span>'
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
        c = counts.get(level, 0)
        if c:
            color = SEVERITY_COLORS.get(level, "#95a5a6")
            pills += (
                f'<span style="background:{color};color:#fff;'
                f'padding:2px 8px;border-radius:4px;'
                f'font-size:0.8em;margin-right:4px;">'
                f'{level.upper()}: {c}</span>'
            )
    return pills or '<span style="color:#7f8c8d;">No findings</span>'
