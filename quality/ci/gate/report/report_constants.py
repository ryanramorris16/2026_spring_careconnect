# File: quality/ci/gate/reports/report_constants.py
# ==========================================================
# Shared constants for the report package.
# ==========================================================

_SECRETS      = "Secrets Scan"
_SAST_JAVA    = "SAST — Java"
_SAST_MULTI   = "SAST — Multi"
_SAST_FLUTTER = "SAST — Flutter"
_SCA_MULTI    = "SCA — Multi"
_QUALITY_GATE = "Quality Gate"

CATEGORY_MAP: dict[str, str] = {
    "trufflehog":       _SECRETS,
    "gitleaks":         _SECRETS,
    "checkstyle":       _SAST_JAVA,
    "pmd":              _SAST_JAVA,
    "spotbugs":         _SAST_JAVA,
    "semgrep":          _SAST_MULTI,
    "flutter_analyze":  _SAST_FLUTTER,
    "dependency_check": _SCA_MULTI,
    "sonar":            _QUALITY_GATE,
}

SEVERITY_COLORS: dict[str, str] = {
    "critical": "#7c0000",
    "high":     "#c0392b",
    "medium":   "#e67e22",
    "low":      "#f1c40f",
    "info":     "#3498db",
}

_MD_TABLE_HEADER    = "| Field | Value |"
_MD_TABLE_SEPARATOR = "|-------|-------|"
