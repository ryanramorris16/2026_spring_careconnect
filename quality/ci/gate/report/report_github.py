# File: quality/ci/gate/report/report_github.py
# ==========================================================
# GitHub PR Comment Poster
# ----------------------------------------------------------
# Posts or updates a single PR comment with the markdown
# report body. Identified by PR_COMMENT_MARKER so it never
# spams the PR with duplicate comments.
#
# Functions:
#   post_or_update_pr_comment(body, env) → None
# ==========================================================

import requests

from quality.ci.gate.report.report_md import PR_COMMENT_MARKER

_API_BASE = "https://api.github.com"


def _gh_headers(token: str) -> dict:
    return {
        "Authorization":        f"Bearer {token}",
        "Accept":               "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def _find_existing_comment(repository: str, pr_number: str,
                            headers: dict) -> int | None:
    page = 1
    while True:
        resp = requests.get(
            f"{_API_BASE}/repos/{repository}/issues/{pr_number}/comments",
            headers=headers,
            params={"per_page": 100, "page": page},
            timeout=15,
        )
        if resp.status_code != 200:
            print(f"[report] Warning: could not list PR comments "
                  f"(HTTP {resp.status_code}).")
            return None

        comments = resp.json()
        if not comments:
            return None

        for comment in comments:
            login = (comment.get("user") or {}).get("login", "")
            if (login == "github-actions[bot]" and
                    PR_COMMENT_MARKER in (comment.get("body") or "")):
                return comment["id"]

        if len(comments) < 100:
            return None
        page += 1


def _upsert_comment(repository: str, pr_number: str,
                    headers: dict, body: str,
                    existing_id: int | None) -> None:
    if existing_id:
        resp = requests.patch(
            f"{_API_BASE}/repos/{repository}/issues/comments/{existing_id}",
            headers=headers,
            json={"body": body},
            timeout=15,
        )
        if resp.status_code == 200:
            print(f"[report] PR comment updated (id={existing_id}).")
        else:
            print(f"[report] Warning: failed to update PR comment "
                  f"(HTTP {resp.status_code}).")
    else:
        resp = requests.post(
            f"{_API_BASE}/repos/{repository}/issues/{pr_number}/comments",
            headers=headers,
            json={"body": body},
            timeout=15,
        )
        if resp.status_code == 201:
            print("[report] PR comment created.")
        else:
            print(f"[report] Warning: failed to create PR comment "
                  f"(HTTP {resp.status_code}).")


def post_or_update_pr_comment(body: str, env: dict) -> None:
    token      = env["token"]
    repository = env["repository"]
    pr_number  = env["pr_number"]

    if not all([token, repository, pr_number]):
        print("[report] Skipping PR comment — missing GITHUB_TOKEN, "
              "GITHUB_REPOSITORY, or PR_NUMBER.")
        return

    headers     = _gh_headers(token)
    existing_id = _find_existing_comment(repository, pr_number, headers)
    _upsert_comment(repository, pr_number, headers, body, existing_id)
