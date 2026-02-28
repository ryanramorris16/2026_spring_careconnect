# File: quality/local/package-report.py
# ==========================================================
# Package Local Report
# ----------------------------------------------------------
# Zips the HTML report and raw XML artifacts into a single
# timestamped archive saved to ~/Downloads/.
#
# Zip structure:
#   local-report.html
#   raw/
#     checkstyle.xml
#     pmd.xml
#     spotbugs.xml
#
# Environment variables (set by run-local-checks.sh):
#   WORK_DIR  — temp directory containing report and raw XMLs
#   ZIP_PATH  — full destination path for the zip file
# ==========================================================

import os
import zipfile
from pathlib import Path

# ----------------------------------------------------------
# Environment
# ----------------------------------------------------------
WORK_DIR = os.environ["WORK_DIR"]
ZIP_PATH = os.environ["ZIP_PATH"]

REPORT_HTML = Path(WORK_DIR) / "local-report.html"
RAW_CS      = Path(WORK_DIR) / "checkstyle.xml"
RAW_PMD     = Path(WORK_DIR) / "pmd.xml"
RAW_SB      = Path(WORK_DIR) / "spotbugs.xml"

# ----------------------------------------------------------
# Ensure Downloads directory exists
# ----------------------------------------------------------
Path(ZIP_PATH).parent.mkdir(parents=True, exist_ok=True)

# ----------------------------------------------------------
# Build zip
# ----------------------------------------------------------
with zipfile.ZipFile(ZIP_PATH, "w", zipfile.ZIP_DEFLATED) as zf:
    if REPORT_HTML.exists():
        zf.write(REPORT_HTML, "local-report.html")
    if RAW_CS.exists():
        zf.write(RAW_CS,  "raw/checkstyle.xml")
    if RAW_PMD.exists():
        zf.write(RAW_PMD, "raw/pmd.xml")
    if RAW_SB.exists():
        zf.write(RAW_SB,  "raw/spotbugs.xml")

print(f"[package-report] Zip saved to: {ZIP_PATH}")
