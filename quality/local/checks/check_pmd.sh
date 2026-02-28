#!/bin/sh
# File: quality/local/check-pmd.sh
# ==========================================================
# PMD — Local Check
# ----------------------------------------------------------
# Runs PMD against backend/core/src/main/java.
# Downloads the PMD zip to quality/tools/ on first run.
#
# Arguments:
#   $1 — REPO_ROOT
#   $2 — WORK_DIR  (temp directory for raw output)
#   $3 — TOOLS_DIR (quality/tools/)
#
# Output:
#   $2/pmd.xml
#
# Exit codes:
#   0 — passed (no violations)
#   1 — failed (violations found) or skipped
# ==========================================================

set -eu

REPO_ROOT="$1"
WORK_DIR="$2"
TOOLS_DIR="$3"

JAVA_SRC="${REPO_ROOT}/backend/core/src/main/java"

PMD_VERSION="6.55.0"
PMD_DIR="${TOOLS_DIR}/pmd-bin-${PMD_VERSION}"
PMD_ZIP="${TOOLS_DIR}/pmd-${PMD_VERSION}.zip"
PMD_URL="https://github.com/pmd/pmd/releases/download/pmd_releases/${PMD_VERSION}/pmd-bin-${PMD_VERSION}.zip"

OUT="${WORK_DIR}/pmd.xml"

# Initialize empty artifact
echo "<pmd></pmd>" > "${OUT}"

# Check java is available
if ! command -v java > /dev/null 2>&1; then
  echo "⚠️  java not installed — skipping PMD."
  exit 0
fi

# Check source directory exists
if [ ! -d "${JAVA_SRC}" ]; then
  echo "⚠️  src/main/java not found — skipping PMD."
  exit 0
fi

# Download zip if missing
if [ ! -f "${PMD_ZIP}" ]; then
  echo "📥 Downloading PMD ${PMD_VERSION}..."
  mkdir -p "${TOOLS_DIR}"
  curl -sSL -o "${PMD_ZIP}" "${PMD_URL}"
  echo "✅ PMD downloaded."
fi

# Extract if missing
if [ ! -d "${PMD_DIR}" ]; then
  echo "📦 Extracting PMD..."
  unzip -q "${PMD_ZIP}" -d "${TOOLS_DIR}"
  echo "✅ PMD extracted."
fi

# Run PMD
"${PMD_DIR}/bin/run.sh" pmd \
  -d "${JAVA_SRC}" \
  -R category/java/bestpractices.xml,category/java/errorprone.xml,category/java/codestyle.xml \
  -f xml \
  -r "${OUT}" || true

# Count violations
COUNT="$(grep -c "<violation" "${OUT}" || true)"

if [ "${COUNT}" -eq 0 ]; then
  echo "✅ No violations found."
  exit 0
else
  echo "❌ ${COUNT} violation(s) found."
  exit 1
fi
