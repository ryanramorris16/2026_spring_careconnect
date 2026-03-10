# CareConnect CI Quality & Security Gate

> **Cohort:** 2026 Spring  
> **Branch:** `team_d`  
> **Proving Ground:** `team_d-james-cicd-gating`  
> **Target:** `main`

---

# Overview

This directory contains the **CareConnect CI Quality & Security Enforcement subsystem**.

The subsystem performs four primary responsibilities:

1. Normalize raw analysis tool outputs  
2. Apply governance policy rules  
3. Determine merge approval or block status  
4. Generate machine-readable and human-readable reports  

This is **not a simple script**.  
It is a **policy-driven CI governance engine** designed to enforce secure and consistent software delivery.

---

# Migration Checklist (team_d → main)

Before promoting this gate from `team_d` to `main`, complete the following:

### Workflow Changes  
File:

`.github/workflows/build-and-analyze.yml`

Required changes:

- Scope `pull_request` trigger to `main`
- Scope `push` trigger to `main`
- Remove temporary **Job Guard (branch ownership isolation)**
- Delete any **Dummy Artifact** steps
- Ensure **all tools generate real raw artifacts**
- Confirm **tool versions are pinned** (no `:latest` tags)

---

### Policy Changes  
File:

`quality/ci/gate/policy.yaml`

Required changes:

- Confirm `gate.mode: enforce`
- Verify intended `blocking` configuration for each tool
- Remove temporary testing thresholds
- Validate severity enforcement rules

---

### GitHub Configuration

Configure repository protections for production enforcement:

- Enable **branch protection** on `main`
- Require the **quality gate workflow status check**
- Require branches to be **up to date before merging**

Branch protection is what ultimately **prevents merges** when the gate fails.

---

# 1. Workflow Definition

File:
```
.github/workflows/build-and-analyze.yml

```
Required changes:

- Update triggers from `team_d` to `main`
- Scope `pull_request` events to `main`
- Remove the temporary **Job Guard**
- Remove the **Create Dummy Artifacts** step
- Ensure tools write artifacts to:

```
quality/analysis/raw/

```
- Verify **pinned versions** for all tools.

Floating tags (`:latest`) must not be used in production CI.

---

# 2. Policy Configuration

File:
```
quality/ci/gate/policy.yaml

```

Key requirements:

- `gate.mode` must be **enforce**
- All enforcement thresholds defined here
- All integrated tools must appear in the policy

Policy is **the governance layer**.

Do **not** hardcode enforcement rules in Python files.

---

# 3. Tool Version Pinning

File:
```
.github/workflows/build-and-analyze.yml

```
Required change:

Replace floating versions with pinned versions to ensure **deterministic builds**.

Example:
```
trufflesecurity/trufflehog:v3.63.2

```
not:

```
trufflesecurity/trufflehog:latest

```

---

# Architecture

### Pipeline Flow

```
Tools → Raw Artifacts → Normalization → Policy Engine → Gate → Reports

```

### Directory Structure:

```
quality/
└── ci/
    └── gate/
        ├── policy.yaml
        ├── schemas.py
        ├── normalize.py
        ├── policy_engine.py
        ├── gate.py
        ├── parsers/
        └── README.md

```

---

# Integrated Tooling Overview

| Layer | Tool | Category | Purpose |
|------|------|------|------|
| Local | flutter analyze | SAST (Dart) | Developer linting before commit |
| CI | TruffleHog | Secrets | Detect committed credentials |
| CI | Gitleaks | Secrets | Detect working-tree secrets |
| CI | Checkstyle | SAST (Java) | Java style enforcement |
| CI | SpotBugs | SAST (Java) | Bytecode bug detection |
| CI | PMD | SAST (Java) | Source misuse detection |
| CI | Semgrep | SAST (Multi-language) | Security pattern detection |
| CI | Pylint | SAST (Python) | Python correctness and style |
| CI | Bandit | Security SAST (Python) | Python vulnerability detection |
| CI | HTMLHint | Web SAST | HTML static analysis |
| CI | Stylelint | Web SAST | CSS static analysis |
| CI | OWASP Dependency-Check | SCA | Dependency CVE detection |
| CI | Sonar | Quality Gate | Centralized quality scoring |
| Post-Deploy | OWASP ZAP | DAST | Runtime vulnerability scanning |

---

# Raw Artifact Requirements

Raw artifacts must be written to:

```
quality/analysis/raw/

```

Expected filenames:
 - trufflehog.jsonl
 - gitleaks.json
 - flutter_analyze.json
 - checkstyle.xml
 - pmd.xml
 - spotbugs.xml
 - semgrep.json
 - pylint.json
 - bandit.json
 - htmlhint.json
 - stylelint.json
 - dependency_check.json
 - sonar.json

---

# Generated Outputs

The gate generates reports in:

```
quality/analysis/

```

Primary outputs:
- report.md
- report.html
- normalized/normalized.json
- evaluated/evaluated.json

The entire directory is uploaded as a CI artifact.

---

# Enforcement Rules

The enforcement model is **fail-safe by design**.

Rules:

- Any tool with `blocking: true` that violates policy will **fail the gate**
- Runtime errors are treated as violations
- Missing artifacts are treated as violations
- Tools run **independently**
- Tool exit codes do **not** control CI
- Only **gate.py** determines final approval

Important:

The gate exits with a **non-zero exit code** in `enforce` mode.

GitHub **branch protection rules** determine whether that failed check prevents merging.

---

# Enabling Branch Protection

To enforce merge blocking:

1. Go to **Settings → Branches**
2. Create a rule for `main`
3. Enable:

 **“Require branches to be up to date before merging.”**

4. Add the CI gate workflow check
5. Enable:

**"Require branches to be up to date before merging."**

---

# Severity Policy

Security findings follow the CVSS model.

| Severity | CVSS Score | Action |
|------|------|------|
| Critical | 9.0–10.0 | Merge blocked |
| High | 7.0–8.9 | Merge blocked |
| Medium | 4.0–6.9 | Reported |
| Low | 0.0–3.9 | Reported |

Final enforcement is controlled by `policy.yaml`.

---

# Exclusion Documentation

When a vulnerability cannot be remediated immediately, document it here.

| Finding ID | Tool | Severity | File / Dependency | Rule / CVE | Rationale | Impact | Remediation Plan | Date | Approved By |
|------|------|------|------|------|------|------|------|------|------|
| _(none yet)_ | — | — | — | — | — | — | — | — | — |

All exclusions require PR review and team lead approval.

---

# How to Add a New Tool

### Step 1 — Create a Parser

Create:

```
quality/ci/gate/parsers/.py

```

Parser requirements:

- Read artifact from `quality/analysis/raw`
- Return standardized schema
- Do not apply policy logic

---

### Step 2 — Register Parser

In:

```
normalize.py

```

Add:

```
(“tool_name”, parse_tool_name)

```

---

### Step 3 — Add Policy Entry

In `policy.yaml`:

```
tool_name:
blocking: true
description: “Tool description”
fail_on:
…

```

---

### Step 4 — Update Workflow

Modify:

```
.github/workflows/build-and-analyze.yml

```

Ensure the tool writes artifacts to:

```
quality/analysis/raw/

```

Tools must not terminate CI directly.

Use:

```
|| true

```

when necessary.

---

# Temporarily Disabling a Tool

### Option 1 (Recommended)

Make it advisory:

```
sonar:
blocking: false

```

---

### Option 2

Remove the workflow step temporarily.

Never modify enforcement logic in code.

---

# Runtime Errors

The following are treated as violations:

- Missing artifact
- Parser crash
- Malformed report
- Tool execution failure
- Schema mismatch

This is intentional **fail-safe governance behavior**.

---

# Local Execution

Run locally:

```
python quality/ci/gate/gate.py

```

Review outputs:

```
quality/analysis/report.md
quality/analysis/report.html
quality/analysis/normalized/normalized.json
quality/analysis/evaluated/evaluated.json

```

---

# Required GitHub Secrets

Repository → **Settings → Secrets and Variables → Actions**

| Secret | Required | Purpose |
|------|------|------|
| NVD_API_KEY | Recommended | Faster Dependency-Check CVE downloads |
| SONAR_TOKEN | Optional | Sonar authentication |
| SONAR_HOST_URL | Optional | Self-hosted Sonar server |
| STAGING_URL | DAST only | Target for OWASP ZAP |

---

# Production Philosophy

This subsystem exists to:

- Enforce consistent quality standards
- Prevent insecure merges
- Provide traceability
- Maintain policy-driven governance

Do not bypass enforcement by:

- Editing workflow exit codes
- Removing gate steps
- Hardcoding exceptions

All governance changes must occur through **policy.yaml** and **PR review**.

---
