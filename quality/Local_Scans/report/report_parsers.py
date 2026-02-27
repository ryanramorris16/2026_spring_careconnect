# File: quality/local/report-parsers.py
# ==========================================================
# Report Parsers
# ----------------------------------------------------------
# Parses raw XML artifacts produced by each tool and returns
# a normalized list of findings and severity counts.
#
# Functions:
# parse_checkstyle(path, repo_root) → findings, sev_counts
# parse_pmd(path, repo_root) → findings, sev_counts
# parse_spotbugs(path) → findings, sev_counts
#
# Each finding dict contains:
# severity — critical | high | medium | low | info
# file — relative file path
# line — line number as string
# rule — rule or bug type name
# message — human-readable description
# ==========================================================
import xml.etree.ElementTree as ET
from pathlib import Path

# ----------------------------------------------------------
# Shared helpers
# ----------------------------------------------------------
def _strip_root(path: str, repo_root: str) -> str:
"""Strip repo root prefix from an absolute path."""
if repo_root in path:
return path[len(repo_root):].lstrip("/\\")
return path

def _empty_sev() -> dict:
return {"critical": 0, "high": 0, "medium": 0, "low": 0, "info": 0}

# ----------------------------------------------------------
# Checkstyle
# ----------------------------------------------------------
def parse_checkstyle(path: Path, repo_root: str) -> tuple[list, dict]:
"""
Parse checkstyle.xml.
Native severity mapping:
error → high
warning → medium
info → low
"""
findings: list = []
sev_counts: dict = _empty_sev()
try:

tree = ET.parse(str(path))
for file_el in tree.getroot().findall("file"):
fname = _strip_root(file_el.get("name", "unknown"), repo_root)
for err in file_el.findall("error"):
native = (err.get("severity") or "info").lower()
sev = {"error": "high",
"warning": "medium",
"info": "low"}.get(native, "low")
sev_counts[sev] += 1
findings.append({
"severity": sev,
"file": fname,
"line": err.get("line", "0"),
"rule": (err.get("source") or "").split(".")[-1],
"message": err.get("message", ""),
})
except Exception as e:
print(f"[report-parsers] Warning: could not parse checkstyle.xml: {e}")
return findings, sev_counts

# ----------------------------------------------------------
# PMD
# ----------------------------------------------------------
def parse_pmd(path: Path, repo_root: str) -> tuple[list, dict]:
"""
Parse pmd.xml.
Native priority mapping:
1 → critical
2 → high
3 → medium
4 → low
5 → info
"""
findings: list = []
sev_counts: dict = _empty_sev()
PRIORITY_MAP = {1: "critical", 2: "high", 3: "medium", 4: "low", 5: "info"}
try:
tree = ET.parse(str(path))
for file_el in tree.getroot().findall("file"):
fname = _strip_root(file_el.get("name", "unknown"), repo_root)
for v in file_el.findall("violation"):
priority = int(v.get("priority", "3"))
sev = PRIORITY_MAP.get(priority, "medium")
sev_counts[sev] += 1
findings.append({
"severity": sev,
"file": fname,
"line": v.get("beginline", "0"),
"rule": v.get("rule", "unknown"),
"message": (v.text or "").strip(),
})

except Exception as e:
print(f"[report-parsers] Warning: could not parse pmd.xml: {e}")
return findings, sev_counts

# ----------------------------------------------------------
# SpotBugs
# ----------------------------------------------------------
def parse_spotbugs(path: Path) -> tuple[list, dict]:
"""
Parse spotbugs.xml.
Native priority mapping:
1 → high
2 → medium
3 → low
"""
findings: list = []
sev_counts: dict = _empty_sev()
PRIORITY_MAP = {1: "high", 2: "medium", 3: "low"}
try:
tree = ET.parse(str(path))
for bug in tree.getroot().findall("BugInstance"):
priority = int(bug.get("priority", "2"))
sev = PRIORITY_MAP.get(priority, "medium