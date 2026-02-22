# ==========================================================
# normalize.py
# ----------------------------------------------------------
# Normalization Layer
#
# Reads raw tool artifacts from:
#   quality/analysis/raw/
#
# Produces:
#   quality/analysis/normalized/normalized.json
#
# This layer DOES NOT apply policy.
# It only standardizes tool outputs.
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result

# Import parsers (we will create these next)
from parsers.flutter import parse_flutter
from parsers.checkstyle import parse_checkstyle
from parsers.pmd import parse_pmd
from parsers.spotbugs import parse_spotbugs
from parsers.semgrep import parse_semgrep
from parsers.dependency_check import parse_dependency_check
from parsers.sonarqube import parse_sonarqube


RAW_DIR = Path("quality/analysis/raw")
NORMALIZED_DIR = Path("quality/analysis/normalized")
NORMALIZED_DIR.mkdir(parents=True, exist_ok=True)

OUTPUT_FILE = NORMALIZED_DIR / "normalized.json"


def normalize():
    results = []

    tool_parsers = [
        ("flutter_analyze", parse_flutter),
        ("checkstyle", parse_checkstyle),
        ("pmd", parse_pmd),
        ("spotbugs", parse_spotbugs),
        ("semgrep", parse_semgrep),
        ("dependency_check", parse_dependency_check),
        ("sonarqube", parse_sonarqube),
    ]

    for tool_name, parser in tool_parsers:
        try:
            result = parser(RAW_DIR)
            results.append(result)
        except Exception as e:
            # If parser crashes, mark runtime_error
            error_result = base_tool_result(tool_name)
            error_result["executed"] = False
            error_result["runtime_error"] = True
            error_result["metadata"]["error"] = str(e)
            results.append(error_result)

    with open(OUTPUT_FILE, "w") as f:
        json.dump(results, f, indent=2)

    print(f"Normalized results written to {OUTPUT_FILE}")


if __name__ == "__main__":
    normalize()