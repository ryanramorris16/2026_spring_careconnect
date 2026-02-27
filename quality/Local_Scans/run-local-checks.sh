#!/bin/sh
# File: quality/local/run-local-checks.sh
# ==========================================================
# CareConnect Local Quality Gate — Entry Point
# ----------------------------------------------------------
# Orchestrates all local checks, generates the HTML report,
# packages everything into a zip, and opens the report.
#
# Usage:
# sh quality/local/run-local-checks.sh
#
# Requires: java, mvn, python3
# ==========================================================
set -eu
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
CHECKS_DIR="${SCRIPT_DIR}/checks"
REPORT_DIR="${SCRIPT_DIR}/report"
TOOLS_DIR="${SCRIPT_DIR}/tools"
TIMESTAMP="$(date '+%Y-%m-%d-%H%M%S')"
WORK_DIR="$(python3 -c "import tempfile; print(tempfile.mkdtemp())")"
ZIP_NAME="careconnect-local-report-${TIMESTAMP}.zip"
ZIP_PATH="${HOME}/Downloads/${ZIP_NAME}"
GENERATED_AT="$(date -u '+%Y-%m-%d %H:%M:%S UTC')"
SCAN_USER="$(whoami)"
FAILED=0
CS_STATUS="skipped"
PMD_STATUS="skipped"
SB_STATUS="skipped"
# ----------------------------------------------------------
# Print header
# ----------------------------------------------------------
echo ""
echo "=============================="
echo " CareConnect Local Gate Check"
echo "=============================="
echo ""
# ----------------------------------------------------------
# Run Checkstyle
# ----------------------------------------------------------
echo "--- Checkstyle ---"
if sh "${CHECKS_DIR}/check_checkstyle.sh" "${REPO_ROOT}" "${WORK_DIR}"
"${TOOLS_DIR}"; then
CS_STATUS="passed"
else
CS_STATUS="failed"
FAILED=$((FAILED + 1))
fi
# ----------------------------------------------------------

# Run PMD
# ----------------------------------------------------------
echo ""
echo "--- PMD ---"
if sh "${CHECKS_DIR}/check_pmd.sh" "${REPO_ROOT}" "${WORK_DIR}" "${TOOLS_DIR}";
then
PMD_STATUS="passed"
else
PMD_STATUS="failed"
FAILED=$((FAILED + 1))
fi
# ----------------------------------------------------------
# Run SpotBugs
# ----------------------------------------------------------
echo ""
echo "--- SpotBugs ---"
if sh "${CHECKS_DIR}/check_spotbugs.sh" "${REPO_ROOT}" "${WORK_DIR}"
"${TOOLS_DIR}"; then
SB_STATUS="passed"
else
SB_STATUS="failed"
FAILED=$((FAILED + 1))
fi
# ----------------------------------------------------------
# Export environment for Python scripts
# ----------------------------------------------------------
export WORK_DIR
export ZIP_PATH
export REPO_ROOT
export GENERATED_AT
export SCAN_USER
export CS_STATUS
export PMD_STATUS
export SB_STATUS
export FAILED
# ----------------------------------------------------------
# Generate HTML report
# ----------------------------------------------------------
echo ""
echo "📄 Generating HTML report..."
python3 "${REPORT_DIR}/generate_report.py"
# ----------------------------------------------------------
# Package zip
# ----------------------------------------------------------
echo ""
echo "📦 Packaging report..."
python3 "${REPORT_DIR}/package_report.py"
# ----------------------------------------------------------
# Open in browser
# ----------------------------------------------------------
echo ""

echo "🌐 Opening report in browser..."
python3 "${REPORT_DIR}/open_report.py"
# ----------------------------------------------------------
# Summary
# ----------------------------------------------------------
echo ""
echo "=============================="
echo " Results"
echo "=============================="
printf " Checkstyle "
if [ "${CS_STATUS}" = "passed" ]; then printf "✅ PASSED\n"
elif [ "${CS_STATUS}" = "failed" ]; then printf "❌ FAILED\n"
else printf "⏸️ SKIPPED\n"; fi
printf " PMD "
if [ "${PMD_STATUS}" = "passed" ]; then printf "✅ PASSED\n"
elif [ "${PMD_STATUS}" = "failed" ]; then printf "❌ FAILED\n"
else printf "⏸️ SKIPPED\n"; fi
printf " SpotBugs "
if [ "${SB_STATUS}" = "passed" ]; then printf "✅ PASSED\n"
elif [ "${SB_STATUS}" = "failed" ]; then printf "❌ FAILED\n"
else printf "⏸️ SKIPPED\n"; fi
echo "------------------------------"
if [ "${FAILED}" -eq 0 ]; then
echo " Result: ✅ All checks passed"
else
echo " Result: ❌ ${FAILED} tool(s) failed"
fi
echo "=============================="
echo ""
echo "📦 Report saved to: ${ZIP_PATH}"
echo ""
exit "${FAILED}"