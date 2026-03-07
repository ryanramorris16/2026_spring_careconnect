"""
SYNTHETIC TEST CREDENTIALS — NOT REAL KEYS

Purpose:
    Trigger TruffleHog and Gitleaks secrets detection during
    sandbox/failing gate engine test runs.

    These credentials are intentionally fake and follow known
    secret patterns to validate that secrets scanning tools are
    functioning correctly end-to-end through the gate engine.

WARNING:
    NEVER replace these with real credentials.
    NEVER use SCAN_ROOT=. when this file is in scope.
    This file must only exist under quality/sandbox/failing/.

/2026_spring_careconnect/quality/sandbox/failing/secrets/fake_credentials.py    
"""


# AWS Credentials
# Matches TruffleHog and Gitleaks AWS IAM key patterns.
# Format: AKIA[A-Z0-9]{16} for access key ID.

AWS_ACCESS_KEY_ID: str     = "AKIAIOSFODNN7EXAMPLE"
AWS_SECRET_ACCESS_KEY: str = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"


# GitHub Personal Access Token
# Matches the GitHub PAT pattern (ghp_ prefix).

GITHUB_TOKEN: str = "ghp_exampleFakeTokenForTestingOnly12345"


# Generic High-Entropy API Key
# Matches generic secret patterns used by Semgrep and Gitleaks
# for detecting high-entropy strings in source code.

API_KEY: str = "sk-proj-examplefakekeyfortestingpurposesonly"


# Database Connection String
# Matches patterns for embedded credentials in connection
# strings — a common HIPAA/security audit finding.

DATABASE_URL: str = (
    "postgresql://admin:SuperSecret123!"
    "@prod-db.example.com:5432/careconnect"
)
