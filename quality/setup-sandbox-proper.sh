#!/usr/bin/env bash
set -euo pipefail

# ------------------------------------------------------------
# CareConnect Quality Gate Sandbox Fixtures
# ------------------------------------------------------------
# This script creates deterministic PASSING + FAILING fixtures:
#   quality/sandbox/passing
#   quality/sandbox/failing
#
# Includes:
# - Flutter project (flutter_sandbox) that is analyzable in-place
# - Java project (java-sandbox) with Maven pom + proper package paths
# - Semgrep rules (.semgrep.yml)
# - Secrets fixture for TruffleHog
# ------------------------------------------------------------

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SANDBOX="${ROOT}/quality/sandbox"

PASS="${SANDBOX}/passing"
FAIL="${SANDBOX}/failing"

echo "Repo root: ${ROOT}"
echo "Sandbox:   ${SANDBOX}"

# Clean + recreate
rm -rf "${PASS}" "${FAIL}"
mkdir -p "${PASS}" "${FAIL}"

# ============================================================
# PASSING FIXTURES
# ============================================================

# -------------------------
# Passing: Semgrep config
# -------------------------
cat > "${PASS}/.semgrep.yml" <<'YAML'
rules:
  - id: java-no-runtime-exec
    message: "Avoid Runtime.exec; use ProcessBuilder with strict allowlist"
    languages: [java]
    severity: WARNING
    patterns:
      - pattern: Runtime.getRuntime().exec(...)
  - id: dart-no-cleartext-http
    message: "Avoid cleartext HTTP; use HTTPS"
    languages: [dart]
    severity: WARNING
    patterns:
      - pattern: "http://..."
YAML

# -------------------------
# Passing: Secrets (no secrets)
# -------------------------
mkdir -p "${PASS}/secrets"
cat > "${PASS}/secrets/README.txt" <<'TXT'
Passing secrets fixture.
This directory intentionally contains NO secrets-like strings.
TXT

# -------------------------
# Passing: Flutter project
# -------------------------
mkdir -p "${PASS}/flutter_sandbox/lib"
cat > "${PASS}/flutter_sandbox/.gitignore" <<'TXT'
.dart_tool/
.build/
build/
.pub/
.idea/
.vscode/
*.iml
