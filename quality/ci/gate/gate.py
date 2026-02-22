# ==========================================================
# gate.py
# ----------------------------------------------------------
# Quality Gate Orchestrator
#
# Execution flow:
#   1. Normalize raw tool artifacts
#   2. Apply policy rules
#   3. Generate summary report
#   4. Exit with correct status code
#
# Exit Codes:
#   0 = Approved
#   1 = Blocked
# ==========================================================

import json
from pathlib import Path

from normalize import normalize
from policy_engine import evaluate


ANALYSIS_DIR = Path("quality/analysis")
REPORT_FILE = ANALYSIS_DIR / "report.json"
SUMMARY_FILE = ANALYSIS_DIR / "summary.md"


def generate_summary():
    evaluated_file = ANALYSIS_DIR / "evaluated" / "evaluated.json"

    if not evaluated_file.exists():
        print("No evaluated results found.")
        return

    with open(evaluated_file) as f:
        data = json.load(f)

    overall_block = data["overall_block"]
    results = data["results"]

    lines = []
    lines.append("# CareConnect Quality Gate Report")
    lines.append("")

    if overall_block:
        lines.append("## ❌ MERGE BLOCKED")
    else:
        lines.append("## ✅ MERGE APPROVED")

    lines.append("")
    lines.append("| Tool | Blocking | Violation | Reason |")
    lines.append("|------|----------|-----------|--------|")

    for r in results:
        lines.append(
            f"| {r['tool']} | "
            f"{'Yes' if r['blocking'] else 'No'} | "
            f"{'Yes' if r['policy_violation'] else 'No'} | "
            f"{r['reason'] or '-'} |"
        )

    SUMMARY_FILE.parent.mkdir(parents=True, exist_ok=True)

    with open(SUMMARY_FILE, "w") as f:
        f.write("\n".join(lines))

    with open(REPORT_FILE, "w") as f:
        json.dump(data, f, indent=2)

    print(f"Summary written to {SUMMARY_FILE}")
    print(f"Report written to {REPORT_FILE}")


def main():
    print("Running normalization...")
    normalize()

    print("Applying policy rules...")
    blocked = evaluate()

    print("Generating summary...")
    generate_summary()

    if blocked:
        print("❌ Merge blocked due to policy violations.")
        exit(1)
    else:
        print("✅ Merge approved.")
        exit(0)


if __name__ == "__main__":
    main()