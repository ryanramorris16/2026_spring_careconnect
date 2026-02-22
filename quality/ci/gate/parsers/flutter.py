# ==========================================================
# flutter.py
# ----------------------------------------------------------
# Parses Flutter analyze output.
#
# Expected raw artifact:
# quality/analysis/raw/flutter_analyze.json
#
# The raw JSON must contain:
# {
#   "error_count": int,
#   "warning_count": int
# }
#
# Normalizes into the standard schema.
# ==========================================================

import json
from pathlib import Path

from schemas import base_tool_result


def parse_flutter(raw_dir: Path):
    tool_name = "flutter_analyze"
    result = base_tool_result(tool_name)

    artifact = raw_dir / "flutter_analyze.json"

    if not artifact.exists():
        result["runtime_error"] = True
        return result

    result["executed"] = True

    with open(artifact) as f:
        data = json.load(f)

    error_count = data.get("error_count", 0)
    warning_count = data.get("warning_count", 0)

    result["violation_count"] = error_count + warning_count

    result["severity_counts"]["high"] = error_count
    result["severity_counts"]["low"] = warning_count

    if error_count > 0:
        result["max_severity"] = "high"
    elif warning_count > 0:
        result["max_severity"] = "low"
    else:
        result["max_severity"] = None

    return result