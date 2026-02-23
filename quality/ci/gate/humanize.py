# File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/ci/gate/humanize.py
# ==========================================================
# humanize.py
# ----------------------------------------------------------
# Human-Readable Results Layer (Layer 3 - Presentation)
#
# Purpose:
#   Create a human-friendly, Markdown-based view of tool findings
#   that maps:
#     - tool name
#     - actual error/message
#     - file path (when available)
#     - line number (when available)
#     - contextual code snippet from the repository checkout
#
# Inputs:
#   - quality/analysis/normalized/normalized.json
#       (Produced by normalize.py - Layer 1)
#
#   - quality/analysis/evaluated/evaluated.json
#       (Produced by policy_engine.py - Layer 2)
#
# Outputs:
#   - quality/analysis/human/index.md
#   - quality/analysis/human/<tool>.md (per tool)
#
#   Special case:
#     Sonar (sonarqube OR sonarcloud) writes to:
#       quality/analysis/human/sonar.md
#
# DESIGN PRINCIPLES:
# - This layer does NOT change policy outcomes.
# - It is read-only relative to enforcement.
# - If a file/line is missing, we still render the finding message.
# - Snippets are extracted from the CI runner checkout
#   (deterministic, not developer-machine dependent).
# - Failures in this layer must NEVER break enforcement logic.
# ==========================================================

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Optional


# ==========================================================
# Utility Functions
# ==========================================================

def _read_json(path: Path, default: Any) -> Any:
    """
    Safely read a JSON file.

    Returns:
        Parsed JSON object OR default value if:
            - file missing
            - malformed JSON
            - unexpected exception

    Rationale:
        Human-readable generation must never crash the gate.
    """
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def _safe_int(value: Any) -> Optional[int]:
    """
    Convert value to int safely.

    Returns:
        int value OR None if conversion fails.

    Used for:
        - line number extraction
        - start_line normalization across tools
    """
    try:
        if value is None:
            return None
        return int(str(value))
    except Exception:
        return None


def _repo_relative_path(p: Optional[str]) -> Optional[str]:
    """
    Normalize tool-reported file paths into repo-relative paths.

    Common cases:
      - TruffleHog filesystem reports: /repo/<path>
      - Some tools report: ./<path> or /<path>

    We strip these prefixes so snippet extraction works on the CI checkout.
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
    context: int = 3
) -> Optional[str]:
    """
    Extract a code snippet around a specific line number.

    Args:
        repo_root: root of the repository checkout
        rel_path: relative path reported by tool (will be normalized)
        line: target line number
        context: number of lines above/below to include

    Returns:
        Markdown-formatted code block OR None.

    Safety:
        - Normalizes /repo/... paths to repo-relative
        - Prevents path traversal outside repo_root
        - Returns None if file missing or line invalid
    """
    try:
        rel_path = _repo_relative_path(rel_path) or ""
        if not rel_path:
            return None

        abs_path = (repo_root / rel_path).resolve()
        repo_root_resolved = repo_root.resolve()

        # Guard against path traversal: abs_path must be within repo_root
        if abs_path != repo_root_resolved and repo_root_resolved not in abs_path.parents:
            return None

        if not abs_path.exists() or not abs_path.is_file():
            return None

        lines = abs_path.read_text(
            encoding="utf-8",
            errors="replace"
        ).splitlines()

        if line <= 0 or line > len(lines):
            return None

        start = max(1, line - context)
        end = min(len(lines), line + context)

        width = len(str(end))
        buffer: List[str] = []

        for i in range(start, end + 1):
            prefix = ">" if i == line else " "
            buffer.append(
                f"{prefix} {str(i).rjust(width)} | {lines[i - 1]}"
            )

        return "```text\n" + "\n".join(buffer) + "\n```"

    except Exception:
        # Never break reporting due to snippet failure
        return None


# ==========================================================
# Tool Metadata Helpers
# ==========================================================

def _tool_title(tool: str) -> str:
    """
    Map internal tool key to human-readable display title.
    """
    return {
        "trufflehog": "TruffleHog (Secrets)",
        "checkstyle": "Checkstyle (Java Style)",
        "spotbugs": "SpotBugs (Java Bytecode)",
        "pmd": "PMD (Java Source)",
        "semgrep": "Semgrep (Multi-language SAST)",
        "flutter_analyze": "Flutter Analyze (Dart Linter)",
        "dependency_check": "OWASP Dependency-Check (SCA)",
        "sonarqube": "Sonar (SonarQube / SonarCloud)",
        "sonarcloud": "Sonar (SonarQube / SonarCloud)",
    }.get(tool, tool)


def _tool_page_name(tool: str) -> str:
    """
    Determine output filename for tool page.

    Special rule:
        SonarQube and SonarCloud both write to sonar.md
        to keep documentation stable across hosting changes.
    """
    if tool in ("sonarqube", "sonarcloud"):
        return "sonar.md"
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

    This function:
        1. Loads normalized results
        2. Loads evaluated enforcement decisions
        3. Builds an index page
        4. Builds one page per tool

    NOTE:
      If a parser does not populate "findings", the human report will still
      render an informative summary using counts + metadata, so the output
      never looks empty or "broken" to the team.
    """
    normalized_list = _read_json(normalized_path, default=[])
    evaluated = _read_json(
        evaluated_path,
        default={"overall_block": True, "results": []}
    )

    # ------------------------------------------------------
    # Map tool → enforcement metadata
    # ------------------------------------------------------
    eval_map: Dict[str, Dict[str, Any]] = {}
    for result in (evaluated.get("results") or []):
        tool_key = result.get("tool")
        if tool_key:
            eval_map[tool_key] = result

    # ------------------------------------------------------
    # Create output directory
    # ------------------------------------------------------
    human_dir = analysis_dir / "human"
    human_dir.mkdir(parents=True, exist_ok=True)

    # ------------------------------------------------------
    # Build index.md
    # ------------------------------------------------------
    index_lines: List[str] = [
        "# Human-Readable Results",
        "",
        "This folder maps each tool finding to:",
        "- tool",
        "- actual error/message",
        "- file + line (when available)",
        "- code snippet (when available)",
        "",
        "## Tool Pages",
        "",
    ]

    # ------------------------------------------------------
    # Render per-tool pages
    # ------------------------------------------------------
    for tool_result in normalized_list:
        tool = tool_result.get("tool", "unknown")
        findings = tool_result.get("findings") or []

        page_name = _tool_page_name(tool)
        page_path = human_dir / page_name
        title = _tool_title(tool)

        index_lines.append(
            f"- [{title}](./{page_name}) ({len(findings)} finding(s))"
        )

        lines: List[str] = [f"# {title}", ""]

        # --------------------------------------------------
        # Enforcement Summary
        # --------------------------------------------------
        enforcement = eval_map.get(tool)
        if enforcement:
            lines.extend([
                "## Enforcement",
                "",
                f"- **Blocking:** `{enforcement.get('blocking')}`",
                f"- **Policy violation:** `{enforcement.get('policy_violation')}`",
            ])
            if enforcement.get("reason"):
                lines.append(f"- **Reason:** `{enforcement.get('reason')}`")
            lines.append("")

        # --------------------------------------------------
        # No findings case (still render a useful page)
        # --------------------------------------------------
        if not findings:
            vc = tool_result.get("violation_count", 0)
            executed = tool_result.get("executed", False)
            runtime_error = tool_result.get("runtime_error", False)
            meta = tool_result.get("metadata") or {}

            lines.append("## Summary")
            lines.append("")
            lines.append(f"- **Executed:** `{executed}`")
            lines.append(f"- **Runtime error:** `{runtime_error}`")
            lines.append(f"- **Violation count:** `{vc}`")

            if meta:
                lines.append("")
                lines.append("## Metadata")
                lines.append("")
                lines.append("```json")
                lines.append(json.dumps(meta, indent=2))
                lines.append("```")

            lines.append("")
            lines.append("_No per-finding detail was provided by the parser for this tool._")

            page_path.write_text("\n".join(lines), encoding="utf-8")
            continue

        # --------------------------------------------------
        # Findings rendering
        # --------------------------------------------------
        lines.append("## Findings")
        lines.append("")

        for idx, finding in enumerate(findings, start=1):
            message = (
                finding.get("message")
                or finding.get("error")
                or finding.get("title")
                or "N/A"
            )

            file_path = _repo_relative_path(
                finding.get("file") or finding.get("path")
            )
            line_number = _safe_int(
                finding.get("line")
                or finding.get("start_line")
                or finding.get("startLine")
            )

            severity = finding.get("severity") or finding.get("level")
            rule = finding.get("rule") or finding.get("check_id")
            rule_url = (
                finding.get("rule_url")
                or finding.get("helpUri")
                or finding.get("url")
            )

            snippet = None
            if file_path and line_number:
                snippet = _read_snippet(
                    repo_root,
                    file_path,
                    line_number,
                    context=3
                )

            lines.append(f"### {idx}. {message}")
            lines.append("")

            if severity:
                lines.append(f"- **Severity:** `{severity}`")
            if rule:
                lines.append(f"- **Rule:** `{rule}`")
            if rule_url:
                lines.append(f"- **Rule URL:** `{rule_url}`")
            if file_path:
                lines.append(f"- **File:** `{file_path}`")
            if line_number:
                lines.append(f"- **Line:** `{line_number}`")

            if snippet:
                lines.extend(["", "**Snippet**", "", snippet])

            lines.extend(["", "---", ""])

        page_path.write_text("\n".join(lines), encoding="utf-8")

    # ------------------------------------------------------
    # Write index.md
    # ------------------------------------------------------
    (human_dir / "index.md").write_text(
        "\n".join(index_lines),
        encoding="utf-8"
    )
    