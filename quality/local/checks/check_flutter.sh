#!/bin/sh
# File: quality/local/checks/check_flutter.sh
# ==========================================================
# Flutter Analyze — Local Check
# ----------------------------------------------------------
# Runs flutter analyze against the frontend/ directory.
# Flutter must be installed and on PATH.
#
# Arguments:
#   $1 — REPO_ROOT
#   $2 — WORK_DIR  (temp directory for raw output)
#   $3 — TOOLS_DIR (quality/tools/ — unused, flutter is global)
#
# Output:
#   $2/flutter_analyze.txt
#
# Exit codes:
#   0 — passed (no errors found)
#   1 — failed (errors found) or skipped
# ==========================================================

set -eu

REPO_ROOT="$1"
WORK_DIR="$2"

FLUTTER_ROOT="${REPO_ROOT}/frontend"
OUT="${WORK_DIR}/flutter_analyze.txt"

# Initialize empty artifact
echo "" > "${OUT}"

# Check flutter is available
if ! command -v flutter > /dev/null 2>&1; then
  echo "⚠️  flutter not installed — skipping Flutter Analyze."
  exit 0
fi

# Check frontend directory exists
if [ ! -d "${FLUTTER_ROOT}" ]; then
  echo "⚠️  frontend/ not found — skipping Flutter Analyze."
  exit 0
fi

# Run flutter analyze and capture output
echo "🔍 Running Flutter Analyze..."
(cd "${FLUTTER_ROOT}" && flutter analyze 2>&1) > "${OUT}" || true

# Count errors only — warnings and info do not block
COUNT="$(grep -cE "^\s*error\s+•" "${OUT}" || true)"

if [ "${COUNT}" -eq 0 ]; then
  echo "✅ No errors found."
  exit 0
else
  echo "❌ ${COUNT} error(s) found."
  exit 1
fi
