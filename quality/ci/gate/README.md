# CareConnect CI Quality & Security Gate

## Overview

This directory contains the production-grade Quality & Security Enforcement
subsystem for CareConnect.

This subsystem:

1. Normalizes raw analysis tool outputs.
2. Applies governance policy rules.
3. Determines merge approval or block status.
4. Generates machine-readable and human-readable reports.

This is NOT a simple script.
It is a structured CI governance engine.

---

## Architecture

Pipeline Flow:

    Tools → Raw Artifacts → Normalization → Policy Engine → Gate → Reports

Directory Structure:

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

Raw artifacts must be placed in:

    quality/analysis/raw/

Generated outputs are written to:

    quality/analysis/

---

## Enforcement Rules

- Any tool marked `blocking: true` in policy.yaml that violates its rule
  will BLOCK the merge.
- Any runtime error, skipped execution, or misconfiguration will BLOCK.
- All tools run even if one fails.
- The final decision is made by `gate.py`.

---

## Policy Configuration

All enforcement thresholds are defined in:

    policy.yaml

Do NOT hardcode policy in Python files.

Example rule:

checkstyle:
 - blocking: true
 - fail_on:
 - violation_count: “>0”

 Modify thresholds via pull request only.

---

## How to Add a New Tool

To add a new analysis tool:

1. Create a new parser file in:
   
       parsers/<tool_name>.py

   The parser must:
   - Read raw artifact from quality/analysis/raw/
   - Return the standardized structure defined in schemas.py
   - Never apply policy logic

2. Register the parser in normalize.py:

       ("tool_name", parse_tool_name),

3. Add the tool to policy.yaml:

       tool_name:
         blocking: true
         fail_on:
           ...

4. Update the CI workflow to generate the raw artifact in:
   
       quality/analysis/raw/

5. Test locally:

       python quality/ci/gate/gate.py

---

## How to Temporarily Disable a Tool

There are two safe ways to temporarily disable enforcement.

### Option 1 (Recommended): Make It Advisory

In policy.yaml:

sonarqube:
 - blocking: false

 This keeps the tool running but prevents it from blocking the merge.

This is ideal when:
- Access is pending
- Configuration is incomplete
- Infrastructure dependency is unavailable

### Option 2: Remove From Workflow

If a tool cannot run at all, remove it from the GitHub workflow
temporarily.

If the parser runs but no artifact is produced, it will be treated
as a runtime error and block the merge.

Do NOT:
- Comment out parser logic
- Hardcode exceptions in policy_engine.py
- Bypass enforcement rules

All changes must remain visible in policy.yaml or workflow YAML.

---

## Runtime Errors

The system treats these as violations:

- Missing artifact
- Parser crash
- Malformed report
- Tool execution failure

These are considered governance failures and block the merge.

---

## Local Execution

To run locally:

1. Place tool reports into:

       quality/analysis/raw/

2. Run:

       python quality/ci/gate/gate.py

3. Review:

       quality/analysis/summary.md
       quality/analysis/report.json

---

## Production Philosophy

This subsystem exists to:

- Enforce consistent quality standards
- Prevent insecure merges
- Provide auditability
- Support future extensibility

All policy changes must go through pull request review.

This is governance, not convenience.