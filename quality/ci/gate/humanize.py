# File: quality/ci/gate/humanize.py
# ==========================================================
# humanize.py
# ----------------------------------------------------------
# Human-Readable Results Layer (Layer 3 of the Quality Gate Engine)
#
# Purpose:
#   Create a human-friendly, Markdown-based view of tool findings
#   that maps:
#     - tool name and enforcement status
#     - actual error/message per finding
#     - file path and line number (when available)
#     - contextual code snippet from the repository checkout
#
# Inputs:
#   quality/analysis/normalized/normalized.json
#     Produced by normalize.py (Layer 1).
#     Contains standardized per-tool results with findings[].
#
#   quality/analysis/evaluated/evaluated.json
#     Produced by policy_engine.py (Layer 2).
#     Contains blocking_results[] and non_blocking_results[].
#
# Outputs:
#   quality/analysis/human/index.md       ← summary index of all tools
#   quality/analysis/human/<tool>.md      ← one page per tool
#
# Design Rules:
#   - DOES NOT change policy outcomes (read-only relative to enforcement).
#   - MUST be resilient: failures here must never break enforcement.
#   - If file/line is missing, finding message is still rendered.
#   - Snippets are extracted from the CI runner checkout (deterministic).
# ==========================================================

import json
from pathlib import Path


# ==========================================================
# Utility Functions
# ==========================================================

def _read_json(path: Path, default) -> any:
    """
    Safely read and parse a JSON file.

    Returns the parsed object, or default if the file is missing,
    malformed, or raises any exception. Human report generation
    must never crash the gate engine.
    """
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def _safe_int(value) -> int | None:
    """
    Convert a value to int safely.

    Returns int or None if conversion fails. Used for line number
    normalization across tools that may emit strings or None.
    """
    try:
        if value is None:
            return None
        return int(str(value))
    except Exception:
        return None


def _repo_relative_path(p: str | None) -> str | None:
    """
    Normalize tool-reported file paths into repo-relative paths.

    Strips common prefixes emitted by tools running inside Docker
    or with absolute paths:
      /repo/<path>  → <path>   (TruffleHog Docker mount)
      ./<path>      → <path>   (relative with dot prefix)
      /<path>       → <path>   (absolute without /repo)

    Returns None if the result is empty.
    """
    if not p:
        return None

    s = str(p).strip()

    if s.startswith("/repo/"):
        s = s[len("/repo/"):]
    if s.startswith("./"):
        s = s[2:]
    if s.startswith("/"):
        s = s[1:]

    return s or None


def _read_snippet(
    repo_root: Path,
    rel_path: str,
    line: int,
    context: int = 3,
) -> str | None:
    """
    Extract a code snippet around a specific line number.

    Args:
        repo_root: Root of the repository checkout on the CI runner.
        rel_path:  Repo-relative file path reported by the tool.
        line:      Target line number (1-indexed).
        context:   Number of lines above and below to include.

    Returns:
        Markdown-formatted code block with line numbers and a ">"
        marker on the target line, or None if extraction fails.

    Safety:
        - Normalizes paths before resolution.
        - Guards against path traversal outside repo_root.
        - Returns None on any failure (missing file, bad line, etc.).
    """
    try:
        rel_path = _repo_relative_path(rel_path) or ""
        if not rel_path:
            return None

        abs_path        = (repo_root / rel_path).resolve()
        repo_root_resolved = repo_root.resolve()

        # Path traversal guard — file must be within the repo root
        if abs_path != repo_root_resolved and \
                repo_root_resolved not in abs_path.parents:
            return None

        if not abs_path.exists() or not abs_path.is_file():
            return None

        lines = abs_path.read_text(
            encoding="utf-8", errors="replace"
        ).splitlines()

        if line <= 0 or line > len(lines):
            return None

        start = max(1, line - context)
        end   = min(len(lines), line + context)
        width = len(str(end))
        buffer: list[str] = []

        for i in range(start, end + 1):
            # ">" marks the target line; " " for context lines
            prefix = ">" if i == line else " "
            buffer.append(f"{prefix} {str(i).rjust(width)} | {lines[i - 1]}")

        return "```text\n" + "\n".join(buffer) + "\n```"

    except Exception:
        # Never break reporting due to snippet extraction failure
        return None


# ==========================================================
# Tool Metadata Helpers
# ==========================================================

def _tool_title(tool: str) -> str:
    """
    Map internal tool key to human-readable display title.

    The tool key "sonar" is used regardless of whether the final
    implementation uses SonarQube or SonarCloud — that decision
    is deferred and does not affect this mapping.
    """
    return {
        "trufflehog":       "TruffleHog (Secrets Detection)",
        "checkstyle":       "Checkstyle (Java Style Enforcement)",
        "spotbugs":         "SpotBugs (Java Bytecode Analysis)",
        "pmd":              "PMD (Java Source Analysis)",
        "semgrep":          "Semgrep (Multi-language SAST)",
        "flutter_analyze":  "Flutter Analyze (Dart Static Analyzer)",
        "dependency_check": "OWASP Dependency-Check (SCA)",
        "sonar":            "Sonar (Quality Gate)",
    }.get(tool, tool)


def _tool_page_name(tool: str) -> str:
    """
    Return the output filename for a tool's detail page.

    All tools write to <tool>.md. The sonar key is stable
    regardless of the underlying SonarQube/SonarCloud decision.
    """
    return f"{tool}.md"


# ==========================================================
# Main Entry Function
# ==========================================================

def generate_human_readable_outputs(
    repo_root: Path,
    analysis_dir: Path,
    normalized_path: Path,
    evaluated_path: Path,
) -> None:
    """
    Generate human-readable Markdown reports for each tool.

    Steps:
        1. Load normalized results from normalized.json.
        2. Load evaluated enforcement decisions from evaluated.json.
        3. Build eval_map for O(1) enforcement lookup per tool.
        4. Render one Markdown page per tool.
        5. Render index.md linking all tool pages.

    Args:
        repo_root:       Root of the repository checkout.
        analysis_dir:    Path to quality/analysis/.
        normalized_path: Path to normalized.json.
        evaluated_path:  Path to evaluated.json.

    Contract:
        - Never raises exceptions outward.
        - Missing findings still produce an informative page.
        - Snippet extraction failures are silently skipped.
    """
    # ----------------------------------------------------------
    # Load inputs
    # ----------------------------------------------------------
    normalized_doc = _read_json(normalized_path, default={})
    evaluated_doc  = _read_json(
        evaluated_path,
        default={"overall_block": True, "blocking_results": [], "non_blocking_results": []}
    )

    # Extract results list from normalized wrapper
    normalized_results: list[dict] = normalized_doc.get("results", [])

    # ----------------------------------------------------------
    # Build eval_map: tool_key → evaluated record
    # ----------------------------------------------------------
    # evaluated.json now has two lists: blocking_results and
    # non_blocking_results. We merge both into a single lookup map.
    # ----------------------------------------------------------
    eval_map: dict[str, dict] = {}
    for record in (evaluated_doc.get("blocking_results") or []):
        tool_key = record.get("tool")
        if tool_key:
            eval_map[tool_key] = record
    for record in (evaluated_doc.get("non_blocking_results") or []):
        tool_key = record.get("tool")
        if tool_key:
            eval_map[tool_key] = record

    # ----------------------------------------------------------
    # Create output directory
    # ----------------------------------------------------------
    human_dir = analysis_dir / "human"
    human_dir.mkdir(parents=True, exist_ok=True)

    # Build index lines as we render each tool page
    index_lines: list[str] = [
        "# Human-Readable Quality Gate Results",
        "",
        "Per-tool detail pages with findings, file locations, and code snippets.",
        "",
        "## Tool Pages",
        "",
    ]

    # ----------------------------------------------------------
    # Render one Markdown page per tool
    # ----------------------------------------------------------
    for tool_result in normalized_results:
        tool      = tool_result.get("tool", "unknown")
        findings  = tool_result.get("findings") or []
        page_name = _tool_page_name(tool)
        page_path = human_dir / page_name
        title     = _tool_title(tool)

        index_lines.append(
            f"- [{title}](./{page_name}) — {len(findings)} finding(s)"
        )

        lines: list[str] = [f"# {title}", ""]

        # --------------------------------------------------
        # Enforcement summary block
        # --------------------------------------------------
        enforcement = eval_map.get(tool)
        if enforcement:
            violation = enforcement.get("policy_violation", False)
            blocking  = enforcement.get("blocking", False)
            reason    = enforcement.get("reason")

            lines += [
                "## Enforcement",
                "",
                f"- **Blocking:**         `{blocking}`",
                f"- **Policy violation:** `{violation}`",
            ]
            if reason:
                lines.append(f"- **Reason:**           `{reason}`")
            lines.append("")

        # --------------------------------------------------
        # No findings — render informative summary page
        # --------------------------------------------------
        if not findings:
            vc            = tool_result.get("violation_count", 0)
            executed      = tool_result.get("executed", False)
            runtime_error = tool_result.get("runtime_error", False)
            meta          = tool_result.get("metadata") or {}

            lines += [
                "## Summary",
                "",
                f"- **Executed:**        `{executed}`",
                f"- **Runtime error:**   `{runtime_error}`",
                f"- **Violation count:** `{vc}`",
            ]

            if meta:
                lines += [
                    "",
                    "## Metadata",
                    "",
                    "```json",
                    json.dumps(meta, indent=2),
                    "```",
                ]

            lines += [
                "",
                "_No per-finding detail was provided by the parser for this tool._",
            ]

            page_path.write_text("\n".join(lines), encoding="utf-8")
            continue

        # --------------------------------------------------
        # Findings rendering
        # --------------------------------------------------
        lines += ["## Findings", ""]

        for idx, finding in enumerate(findings, start=1):
            # Extract common finding fields with safe fallbacks
            message = (
                finding.get("message")
                or finding.get("error")
                or finding.get("title")
                or "N/A"
            )
            file_path   = _repo_relative_path(
                finding.get("file") or finding.get("path")
            )
            line_number = _safe_int(
                finding.get("line")
                or finding.get("start_line")
                or finding.get("startLine")
            )
            severity = finding.get("severity") or finding.get("level")
            rule     = finding.get("rule") or finding.get("check_id")
            rule_url = (
                finding.get("rule_url")
                or finding.get("helpUri")
                or finding.get("url")
            )

            # Attempt to extract a code snippet for this finding
            snippet = None
            if file_path and line_number:
                snippet = _read_snippet(
                    repo_root, file_path, line_number, context=3
                )

            # Render finding header and detail fields
            lines += [f"### {idx}. {message}", ""]

            if severity:
                lines.append(f"- **Severity:** `{severity}`")
            if rule:
                lines.append(f"- **Rule:** `{rule}`")
            if rule_url:
                lines.append(f"- **Rule URL:** {rule_url}")
            if file_path:
                lines.append(f"- **File:** `{file_path}`")
            if line_number:
                lines.append(f"- **Line:** `{line_number}`")

            # Render snippet if extraction succeeded
            if snippet:
                lines += ["", "**Code Snippet**", "", snippet]

            lines += ["", "---", ""]

        page_path.write_text("\n".join(lines), encoding="utf-8")

    # ----------------------------------------------------------
    # Write index.md
    # ----------------------------------------------------------
    (human_dir / "index.md").write_text(
        "\n".join(index_lines), encoding="utf-8"
    )

    print(f"[humanize] Human-readable pages written to: {human_dir}")
    