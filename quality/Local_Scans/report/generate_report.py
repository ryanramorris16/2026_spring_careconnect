# File: quality/local/generate-report.py
# ==========================================================
# Generate Local HTML Report — Entry Point
# ----------------------------------------------------------
# Reads parsed results from report-parsers.py and builds
# the HTML report via report-html.py.
# Writes the completed report to WORK_DIR/local-report.html.
#
# Environment variables (set by run-local-checks.sh):
# WORK_DIR — temp directory containing raw XMLs
# REPO_ROOT — repository root path
# GENERATED_AT — UTC timestamp string
# SCAN_USER — local username
# CS_STATUS — passed | failed | skipped
# PMD_STATUS — passed | failed | skipped
# SB_STATUS — passed | failed | skipped
# FAILED — number of failed tools
# ==========================================================
import os
import sys
from pathlib import Path
# ----------------------------------------------------------
# Resolve local module directory so imports work when called
# from any working directory
# ----------------------------------------------------------
LOCAL_DIR = Path(__file__).parent
sys.path.insert(0, str(LOCAL_DIR))
from report_parsers import parse_checkstyle, parse_pmd, parse_spotbugs
from report_html import build_html
# ----------------------------------------------------------
# Environment
# ----------------------------------------------------------
WORK_DIR = os.environ["WORK_DIR"]
REPO_ROOT = os.environ["REPO_ROOT"]
GENERATED_AT = os.environ["GENERATED_AT"]
SCAN_USER = os.environ["SCAN_USER"]
CS_STATUS = os.environ["CS_STATUS"]
PMD_STATUS = os.environ["PMD_STATUS"]
SB_STATUS = os.environ["SB_STATUS"]
FAILED = int(os.environ["FAILED"])
RAW_CS = Path(WORK_DIR) / "checkstyle.xml"
RAW_PMD = Path(WORK_DIR) / "pmd.xml"
RAW_SB = Path(WORK_DIR) / "spotbugs.xml"
OUT = Path(WORK_DIR) / "local-report.html"
# ----------------------------------------------------------
# Parse raw artifacts
# ----------------------------------------------------------
cs_findings, cs_sev = parse_checkstyle(RAW_CS, REPO_ROOT)
pmd_findings, pmd_sev = parse_pmd(RAW_PMD, REPO_ROOT)
sb_findings, sb_sev = parse_spotbugs(RAW_SB)

# ----------------------------------------------------------
# Build HTML
# ----------------------------------------------------------
html = build_html({
"generated_at": GENERATED_AT,
"scan_user": SCAN_USER,
"repo_root": REPO_ROOT,
"failed": FAILED,
"cs_status": CS_STATUS,
"pmd_status": PMD_STATUS,
"sb_status": SB_STATUS,
"cs_findings": cs_findings,
"pmd_findings": pmd_findings,
"sb_findings": sb_findings,
"cs_sev": cs_sev,
"pmd_sev": pmd_sev,
"sb_sev": sb_sev,
})
# ----------------------------------------------------------
# Write output
# ----------------------------------------------------------
OUT.write_text(html, encoding="utf-8")
print(f"[generate-report] HTML written to: {OUT}")