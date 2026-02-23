# CareConnect CI Quality & Security Gate

> **Cohort:** 2026 Spring
> **Branch:** `team_d`
> **Proving Ground:** `team_d-james-cicd-gating`
> **Target:** `main`

---

## Overview

This directory contains the production-grade Quality & Security Enforcement subsystem for CareConnect.

This subsystem:
 1. Normalizes raw analysis tool outputs.
 2. Applies governance policy rules.
 3. Determines merge approval or block status.
 4. Generates machine-readable and human-readable reports.

This is NOT a simple script.
It is a structured, policy-driven CI governance engine.

---

---

## Migration Checklist (team_d → main)

Before promoting this gate from `team_d` to `main`, complete the following:

### Workflow Changes
File:
`.github/workflows/build-and-analyze.yml`

 - Scope `pull_request` to `main`
 - Scope `push` to `main`
 - Remove temporary Job Guard (branch ownership isolation)
 - Delete any Dummy Artifact steps
 - Ensure all tools generate real raw artifacts
 - Confirm tool versions are pinned (no `:latest` tags)
 
### Policy Changes
File:
`quality/ci/gate/policy.yaml`

 - Ensure `gate.mode: enforce`
 - Confirm intended `blocking` configuration for all tools
 - Remove temporary testing thresholds
 - Validate severity enforcement rules

### GitHub Configuration

 - Enable branch protection on `main`
 - Require the quality gate workflow status check
 - Require branches to be up to date before merging

---

## 1. Workflow Definition

File:
```
.github/workflows/build-and-analyze.yml

```

Required changes:
 - Update triggers from team_d to main
 - Scope pull_request to main
 - Remove the temporary Job Guard (branch ownership isolation)
 - Delete the “Create Dummy Artifacts (Temporary)” step
 - Ensure all real tools write raw artifacts to quality/analysis/raw/
 - Verify pinned tool versions (no floating :latest tags)

---

## 2. Policy Configuration

File:
```
quality/ci/gate/policy.yaml

```

Required changes:
 - Ensure gate.mode: enforce
 - Confirm intended blocking settings (especially SonarQube)
 - Remove any temporary testing thresholds
 - Validate that all integrated tools are represented in the policy

---

## 3. Tool Version Pinning

File:
```
.github/workflows/build-and-analyze.yml

```

Required change:
 - Replace floating tags (e.g., :latest) with pinned versions to ensure deterministic production enforcement.

---

## Architecture

### Pipeline Flow:

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

### Integrated Tooling Overview

| Layer | Tool | Category | Purpose |
|-------|------|----------|---------|
| Local | flutter analyze | SAST (Dart) | Developer linting before commit |
| CI | TruffleHog | Secrets | Detect committed credentials |
| CI | Checkstyle | SAST (Java) | Style enforcement |
| CI | SpotBugs | SAST (Java) | Bytecode bug detection |
| CI | PMD | SAST (Java) | Source complexity & misuse |
| CI | Semgrep | SAST (Multi-language) | OWASP & pattern detection |
| CI | OWASP Dependency-Check | SCA | CVE detection in dependencies |
| CI | SonarQube | Quality Gate | Centralized quality scoring |
| Post-Deploy | OWASP ZAP | DAST | Runtime vulnerability scanning |


Raw artifacts must be placed in:

```
quality/analysis/raw/

```

Expected raw artifact filenames:
 - trufflehog.jsonl
 - flutter_analyze.json
 - checkstyle.xml
 - pmd.xml
 - spotbugs.xml
 - semgrep.json
 - dependency_check.json
 - sonarqube.json

Generated outputs are written to:

```
quality/analysis/

```

--- 

## Enforcement Rules
 - Any tool marked blocking: true in policy.yaml that violates its rule will BLOCK the merge.
 - Any runtime error, skipped execution, missing artifact, or misconfiguration will BLOCK the merge (fail-safe design).
 - All tools run even if one fails.
 - Individual tool exit codes do NOT control the workflow.
 - Only `gate.py` exit code determines merge approval.
 - The final decision authority is `gate.py`.
 - Secrets findings (TruffleHog) are treated as high severity by default.
Any finding blocks the merge unless explicitly configured as advisory. 
Branch protection enforces the gate at the repository level; the workflow alone does not prevent merging.

---

### Enabling Branch Protection (GitHub)

To enforce merge blocking based on the quality gate:

1. Go to **Settings → Branches** in your GitHub repository.
2. Add a branch protection rule for:
   - `main`  
   Optionally also for `team_d` in testing phases.
3. Enable **“Require status checks to pass before merging.”**
4. Add the gate workflow’s check (e.g., the relevant GitHub Actions job name) as a required check.
5. Enable **“Require branches to be up to date before merging.”**

---

## Severity Policy

The following severity model applies to CVE-based and security findings:

| Severity | CVSS Score | Action |
|----------|------------|--------|
| Critical | 9.0 – 10.0 | ❌ Hard block — merge prevented |
| High     | 7.0 – 8.9  | ❌ Hard block — merge prevented |
| Medium   | 4.0 – 6.9  | ⚠️ Reported — merge allowed unless overridden |
| Low      | 0.0 – 3.9  | ⚠️ Reported — merge allowed |

Severity enforcement is ultimately governed by `policy.yaml`.

---

## Exclusion Documentation

When a Critical or High finding cannot be remediated immediately, document it below.

| Finding ID | Tool | Severity | File / Dependency | Rule / CVE | Rationale | Potential Impact | Remediation Plan | Date | Approved By |
|------------|------|----------|------------------|------------|-----------|------------------|------------------|------|-------------|
| _(none yet)_ | — | — | — | — | — | — | — | — | — |


Exclusions require pull request review and team lead approval.

---

## Policy Configuration

All enforcement thresholds are defined in:

```
quality/ci/gate/policy.yaml

```

Do NOT hardcode policy rules inside Python files.

Example rule:

```
checkstyle:
  blocking: true
  fail_on:
    violation_count: ">0"

```

Modify thresholds via pull request only.
Policy changes are governance changes and must be reviewed.

---

## How to Add a New Tool

To add a new analysis tool:

### 1. Create a New Parser

Create a parser file in:

```
quality/ci/gate/parsers/<tool_name>.py

```

The parser must:
 - Read the raw artifact from quality/analysis/raw/
 - Return the standardized structure defined in schemas.py
 - Never apply policy logic
 - Never determine blocking status

⸻

### 2. Register the Parser

In `normalize.py`, register the parser:


```
("tool_name", parse_tool_name),

```

This connects the raw artifact to the normalization pipeline.

---

### 3. Add the Tool to Policy

In `policy.yaml`:

```
tool_name:
  blocking: true
  description: "Tool description"
  fail_on:
    ...

```

Policy determines enforcement — not the parser.

---

### 4. Update the CI Workflow

Modify:

```
.github/workflows/build-and-analyze.yml

```

Ensure the tool writes its raw artifact to:

```
quality/analysis/raw/

```

Tools must not terminate the workflow directly.
Use || true if necessary to allow the gate engine to decide.


### 5. Test Locally

```
python quality/ci/gate/gate.py

```

Verify:

```
quality/analysis/summary.md
quality/analysis/report.json

```

---

## How to Temporarily Disable a Tool

There are two safe methods.

### Option 1 (Recommended): Make It Advisory

In `policy.yaml`:

```
sonarqube:
  blocking: false

```

This keeps the tool running but prevents it from blocking the merge.

Use this when:
 - Access is pending
 - Configuration is incomplete
 - Infrastructure dependency is unavailable

To re-enable SonarQube as blocking:
 1. Ensure the workflow generates quality/analysis/raw/sonarqube.json.
 2. Confirm the JSON contains:
  
```
{
  "projectStatus": { "status": "OK" | "ERROR" }
}

```

3. Update policy.yaml:

```
sonarqube:
  blocking: true

```


### Option 2: Remove From Workflow

If a tool cannot run at all, remove it from the GitHub workflow temporarily.

If a parser executes but no artifact is produced, it will be treated as a runtime error and block the merge.

Do NOT:
 - Comment out parser logic
 - Hardcode exceptions in policy_engine.py
 - Bypass enforcement rules in gate.py
 - Modify exit codes to force approval

All changes must remain visible in policy.yaml or workflow YAML.

---

## Runtime Errors

The system treats the following as violations:
 - Missing artifact
 - Parser crash
 - Malformed report
 - Tool execution failure
 - Unexpected schema mismatch

These are considered governance failures and block the merge.

This is intentional fail-safe behavior.

---

## Local Execution

### To run locally:
 1. Place tool reports into:

```
quality/analysis/raw/

```

 2. Run:

```
python quality/ci/gate/gate.py

```

 3. Review:

```
quality/analysis/summary.md
quality/analysis/report.json
quality/analysis/normalized/normalized.json
quality/analysis/evaluated/evaluated.json

```

---

### Report Outputs

The gate engine produces:
 - `quality/analysis/summary.md`      (Human-readable summary)
 - `quality/analysis/report.json`     (Machine-readable evaluation)
 - `quality/analysis/report.md`       (Rich CI report)
 - `quality/analysis/normalized/`     (Layer 1 output)
 - `quality/analysis/evaluated/`      (Layer 2 output)

The entire quality/analysis/ directory is uploaded as a CI artifact.

---

## Required GitHub Secrets

Go to: **Settings → Secrets and Variables → Actions**

| Secret         | Required        | Purpose |
|----------------|-----------------|---------|
| `NVD_API_KEY`  | Recommended     | Speeds up OWASP Dependency-Check CVE downloads. Without it, scanning is rate-limited and significantly slower. |
| `SONAR_TOKEN`  | Optional (to enable Sonar) | Authentication token for SonarCloud/SonarQube. |
| `SONAR_HOST_URL` | Optional (self-hosted only) | SonarQube server URL (not required for SonarCloud). |
| `STAGING_URL` | DAST only      | AWS staging ALB DNS name used by OWASP ZAP. |

**Notes:**
 - If `SONAR_TOKEN` is missing, Sonar runs in advisory mode or is skipped, but the gate engine still runs.
 - If `NVD_API_KEY` is missing, OWASP Dependency-Check still runs but will be very slow due to NVD rate limiting.
 - `STAGING_URL` is only used by the DAST job and not required for pull request gating.

---

---

## SonarQube / SonarCloud Integration Notes

Sonar is currently configured as advisory because no active server or authentication token is configured.

Sonar integration is executed through:

```
.github/workflows/build-and-analyze.yml

```

The workflow must invoke Sonar analysis and generate a normalized status artifact for the gate engine.

To enable Sonar as a blocking quality gate, complete the following steps.


### Step 1 — Choose SonarCloud or Self-Hosted SonarQube

Sonar must be reachable from GitHub Actions and capable of returning a quality gate status.


### Step 2 — Configure Sonar Project

#### SonarCloud Setup
1. Sign in to https://sonarcloud.io using GitHub.
2. Import the CareConnect repository.
3. Generate a ***SONAR_TOKEN***.
4. Add the token in GitHub:
	• Settings → Secrets and Variables → Actions → New Repository Secret
	• Name: SONAR_TOKEN

No SONAR_HOST_URL is required for SonarCloud.

⸻

#### Self-Hosted SonarQube Setup
1. Deploy a SonarQube server.
2. Create a project in SonarQube.
3. Generate a user token.
4. Add the following GitHub secrets:
	• SONAR_TOKEN
	• SONAR_HOST_URL (example: https://sonar.company.com)

### Step 3 — Configure Maven Project

In:

```
backend/core/pom.xml

```

Ensure the following exists:

```
<properties>
  <sonar.projectKey>careconnect</sonar.projectKey>
</properties>

```

Ensure the Sonar Maven plugin is executed in:

```
.github/workflows/build-and-analyze.yml

```

The workflow must call Sonar during the CI build phase.


### Step 4 — Ensure CI Generates Required Artifact

The workflow must write the Sonar quality gate result to:

```
quality/analysis/raw/sonarqube.json

```

Expected structure:

```
{
  "projectStatus": {
    "status": "OK" | "ERROR"
  }
}

```

This file is consumed by the normalization layer:

```
quality/ci/gate/normalize.py

```

and evaluated by:

```
quality/ci/gate/policy_engine.py

```

### Step 5 — Enable Blocking Enforcement

In:

```
quality/ci/gate/policy.yaml

```

Set:

```
sonarqube:
  blocking: true

```

Commit via pull request.

### Step 6 — Validate Integration
1. Open a test PR.
2. Confirm Sonar analysis runs in:
`.github/workflows/build-and-analyze.yml`
3. Confirm `sonarqube.json` is generated in:
`quality/analysis/raw/`
4. Confirm the gate blocks the merge if Sonar returns ERROR.

Once validated, Sonar becomes a fully enforced quality gate within the CI governance engine.

---

## Production Philosophy

This subsystem exists to:
 - Enforce consistent quality standards
 - Prevent insecure merges
 - Provide auditability
 - Support structured scalability
 - Separate governance from implementation logic

All policy changes must go through pull request review.

Do not bypass this system by:
 - Editing workflow exit codes
 - Removing the gate step from CI
 - Hardcoding exceptions in enforcement logic

All enforcement changes must be performed via policy.yaml or documented workflow modifications.

This is governance, not convenience.


---

---

## Appendix A — Local Tool Execution Reference

### Java (backend/core)

```
./mvnw checkstyle:check  
./mvnw spotbugs:check  
./mvnw pmd:check  
./mvnw org.owasp:dependency-check-maven:check -Dformat=ALL -Dnvd.api.key=YOUR_NVD_API_KEY  

```

---

### Flutter (frontend)

```
flutter pub get  
flutter analyze  
dart format --set-exit-if-changed .

```

---

### Semgrep (repository root)

```
pip install semgrep  
semgrep --config p/default --config p/owasp-top-ten --config p/secrets .

```

---
