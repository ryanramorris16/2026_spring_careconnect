# CareConnect Quality Gate — Sandbox Test Cases
# ==========================================================
# File: quality/ci/gate/tests/TEST_CASES.md
#
# Purpose:
#   Documents every intentional finding in the sandbox fixtures
#   and defines the expected gate outcome for each sandbox.
#
# Sandboxes:
#   quality/sandbox/failing  → gate should BLOCK (overall_block=true)
#   quality/sandbox/passing  → gate should PASS  (overall_block=false)
#
# How to run:
#   Set SCAN_ROOT in .env, push to a team_d branch, and verify
#   the gate outcome matches the Expected Result column.
#
#   Failing sandbox:  SCAN_ROOT=quality/sandbox/failing
#   Passing sandbox:  SCAN_ROOT=quality/sandbox/passing
# ==========================================================

---

## Sandbox: `quality/sandbox/failing`

**Expected gate outcome: 🚫 BLOCKED**

Every tool with `blocking: true` in policy.yaml should report at
least one violation. The overall gate must exit with code 1.

---

### TruffleHog — Secrets Detection

**Source files:**
- `secrets/secret.txt`
- `secrets/credentials.env`

| # | Finding | File | Expected Severity |
|---|---------|------|-------------------|
| 1 | AWS Access Key ID (`AKIAIOSFODNN7EXAMPLE`) | `secrets/secret.txt` | critical (if verified) / high (unverified) |
| 2 | AWS Secret Access Key | `secrets/secret.txt` | critical / high |
| 3 | AWS Access Key ID (`AKIAIOSFODNN7ABCD1234`) | `secrets/secret.txt` | critical / high |
| 4 | GitHub Token (`ghp_xxx...`) | `secrets/secret.txt` | critical / high |
| 5 | AWS Access Key ID | `secrets/credentials.env` | critical / high |
| 6 | AWS Secret Access Key | `secrets/credentials.env` | critical / high |
| 7 | GitHub Token | `secrets/credentials.env` | critical / high |

**Notes:**
- `DB_PASSWORD=Password123!` and `API_KEY=SECRET_BACKEND_KEY_999999` may or
  may not trigger TruffleHog depending on detector coverage.
- The `-----BEGIN PRIVATE KEY-----` header in `InsecureRandom.java` may
  trigger an additional finding.
- `CryptoWeakness.java` contains `SECRET_DEMO_JAVA_TOKEN_12345` — may trigger.
- All sandbox secrets are intentionally fake/example values. None are real.
- TruffleHog self-referential filtering (scanning its own JSONL output)
  is handled in `parsers/trufflehog.py`.

**Expected policy outcome:** `finding_present` violation → BLOCK

---

### Flutter Analyze — Dart Static Analyzer

**Source files:**
- `flutter_sandbox/lib/main.dart`
- `flutter_sandbox/lib/services/api_service.dart`
- `flutter_sandbox/lib/utils/helper.dart`

| # | Finding | File | Line | Expected Native Severity |
|---|---------|------|------|--------------------------|
| 1 | Unused local variable `unused` | `main.dart` | ~19 | warning / info |
| 2 | Condition is always `true` (`if (true)`) | `main.dart` | ~22 | warning |
| 3 | Unused local variable `unusedLocal` | `api_service.dart` | ~8 | warning / info |
| 4 | Pointless condition (`if (a == a)`) | `helper.dart` | ~5 | warning |
| 5 | Unused local variable `temp` | `helper.dart` | ~9 | warning / info |

**Notes:**
- `flutter analyze --machine` severity levels are: `error`, `warning`, `info`, `hint`.
- `warning` normalizes to `medium`, `info`/`hint` normalize to `low`/`info`.
- The policy `error_count: ">0"` rule fires on `high` severity (analyzer errors).
  If these produce only warnings, the `error_count` rule will NOT trigger.
- The `violation_count: ">0"` rule (if configured) will trigger on any finding.
- Exact findings depend on the Dart SDK version and analysis_options.yaml.

**Expected policy outcome:** `flutter_errors_present` or `violation_count_exceeded`
depending on policy.yaml configuration → BLOCK

---

### Checkstyle — Java Style Enforcement

**Source files:**
- All `.java` files in `java-sandbox/src/main/java/`

Checkstyle runs with Google's style rules (`/google_checks.xml`).
Common violations across the failing Java files:

| # | Finding Type | Likely Files | Expected Severity |
|---|-------------|--------------|-------------------|
| 1 | Missing Javadoc on public methods/classes | Most files | high (error) |
| 2 | Line length exceeded | Multiple files | high (error) |
| 3 | Import ordering | Files with imports | medium (warning) |
| 4 | Naming conventions (constants, variables) | Multiple files | high (error) |
| 5 | Whitespace / indentation | Multiple files | high / medium |
| 6 | Missing braces on single-line blocks | `SqlInjection.java`, `CommandInjection.java` | high |

**Notes:**
- Checkstyle findings are style violations, not security issues.
- Google checks are strict — most of the failing sandbox files will generate
  multiple errors per file due to missing Javadoc and formatting.
- The passing sandbox Java files include Javadoc and follow Google style.

**Expected policy outcome:** `violation_count_exceeded` or `severity_high_and_above` → BLOCK

---

### PMD — Java Source Static Analysis

**Source files:**
- All `.java` files in `java-sandbox/src/main/java/`

| # | Finding | File | PMD Rule | Expected Severity |
|---|---------|------|----------|-------------------|
| 1 | Empty catch block | `SqlInjection.java` | `EmptyCatchBlock` | high (priority 2) |
| 2 | Empty catch block | `Bad.java` | `EmptyCatchBlock` | high (priority 2) |
| 3 | Empty method body | `ResourceLeak.java` | `EmptyMethodBody` | high (priority 2) |
| 4 | Pointless string concatenation (`"" + userInput`) | `CommandInjection.java` | `UselessStringValueOf` or `AppendCharacterWithChar` | medium (priority 3) |
| 5 | Unused local variable `unused` | `Bad.java` | `UnusedLocalVariable` | medium (priority 3) |
| 6 | Unused local variable `unused` (int) | `Bad.java` | `UnusedLocalVariable` | medium (priority 3) |
| 7 | Null check after method call | `CommandInjection.java` | Various | medium |
| 8 | System.out.println usage | `Bad.java`, `InsecureRandom.java` | `SystemPrintln` | medium (priority 3) |

**Notes:**
- PMD priority maps: 1→critical, 2→high, 3→medium, 4→low, 5→info.
- Actual findings depend on which rulesets are active in the workflow
  (`bestpractices.xml`, `errorprone.xml`, `codestyle.xml`).
- More violations likely exist across all failing sandbox files.

**Expected policy outcome:** `severity_high_and_above` or `violation_count_exceeded` → BLOCK

---

### SpotBugs — Java Bytecode Analysis

**Source files (compiled `.class` files):**
- `java-sandbox/target/classes/com/careconnect/sandbox/`

| # | Finding | File | SpotBugs Pattern | Expected Severity |
|---|---------|------|-----------------|-------------------|
| 1 | SQL built via string concatenation | `SqlInjection.java` | `SQL_INJECTION` / `SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE` | high (priority 1) |
| 2 | Command injection via `Runtime.exec()` | `CommandInjection.java` | `COMMAND_INJECTION` | high (priority 1) |
| 3 | Weak hash algorithm (MD5) | `CryptoWeakness.java`, `Bad.java` | `WEAK_MESSAGE_DIGEST_MD5` | high (priority 1) |
| 4 | Predictable random seed | `InsecureRandom.java` | `PREDICTABLE_RANDOM` | high (priority 1) |
| 5 | Null dereference guaranteed | `NullDeref.java` | `NP_ALWAYS_NULL` | high (priority 1) |
| 6 | Resource leak (stream not closed) | `ResourceLeak.java` | `OBM_UNCALLED_METHOD` / `OS_OPEN_STREAM` | medium (priority 2) |
| 7 | Lazy singleton without synchronization | `ThreadUnsafeSingleton.java` | `LI_LAZY_INIT_STATIC` | medium (priority 2) |
| 8 | `equals()` defined without `hashCode()` | `BadEqualsHashCode.java` | `HE_EQUALS_NO_HASHCODE` | medium (priority 2) |
| 9 | Possible null dereference on method return | `Bad.java` | `NP_NULL_ON_SOME_PATH` | high (priority 1) |

**Notes:**
- SpotBugs analyzes compiled bytecode. Pre-compiled `.class` files are
  included in the sandbox `target/classes/` directory.
- Priority 1 → high, priority 2 → medium, priority 3 → low.
- SQL injection and command injection are priority 1 (high) findings.

**Expected policy outcome:** `severity_high_and_above` → BLOCK

---

### Semgrep — Multi-language SAST

**Source files:**
- All files under `java-sandbox/` and `flutter_sandbox/`
- Custom rules loaded from `.semgrep.yml`

| # | Finding | File | Rule ID | Expected Severity |
|---|---------|------|---------|-------------------|
| 1 | Hardcoded secret-like token (`SECRET_DEMO_KEY_...`) | `Bad.java` | `demo.hardcoded-secret` | high (ERROR) |
| 2 | Hardcoded secret-like token (`SECRET_DEMO_JAVA_TOKEN_...`) | `CryptoWeakness.java` | `demo.hardcoded-secret` | high (ERROR) |
| 3 | Insecure hash algorithm MD5 | `CryptoWeakness.java` | `demo.insecure-hash` | high (ERROR) |
| 4 | Insecure hash algorithm MD5 | `Bad.java` | `demo.insecure-hash` | high (ERROR) |

**Notes:**
- The failing sandbox `.semgrep.yml` defines two custom rules with severity `HIGH`.
- Semgrep `--config=auto` also runs community rules which may generate
  additional findings beyond the custom rules above.
- `ERROR` normalizes to `high` in the gate engine.
- The `UserService.java` hardcoded password (`HardcodedPassword!`) may also
  trigger community rules.

**Expected policy outcome:** `severity_high_and_above` → BLOCK

---

### OWASP Dependency-Check — SCA

**Source files:**
- `java-sandbox/pom.xml`
- `flutter_sandbox/pubspec.yaml`

Dependency-Check scans declared dependencies for known CVEs.

**Notes:**
- The `pom.xml` and `pubspec.yaml` in the failing sandbox should declare
  at least one dependency with a known CVE for a meaningful test.
- If the sandbox pom.xml uses current/unvulnerable dependency versions,
  Dependency-Check may report zero findings even in the failing sandbox.
- **Action required:** Review `pom.xml` and `pubspec.yaml` and add at least
  one intentionally outdated dependency with a known CVE.
  Example: `spring-core:5.3.18` (CVE-2022-22965, Spring4Shell).
- Without a vulnerable dependency, this tool will pass even in the failing
  sandbox, which is misleading for test validation.

**Expected policy outcome (if vulnerable dep present):** `vulnerability_present` → BLOCK
**Current likely outcome (if no known-vulnerable dep):** PASS (advisory only or no violation)

---

### Sonar — Quality Gate

**Status: DISABLED**

Sonar is not yet configured. The parser returns `executed=False` with
`metadata.status=disabled` and is routed to `non_blocking_results`.
It will never block the merge regardless of sandbox or policy configuration.

**Expected policy outcome:** Disabled → no violation → PASS (advisory only)

---

## Sandbox: `quality/sandbox/passing`

**Expected gate outcome: ✅ APPROVED**

All tools should report zero violations (or only disabled tools).
The overall gate must exit with code 0.

---

### TruffleHog — No Secrets

**Source:** `secrets/README.txt`

Contents: `"This folder intentionally contains no secrets."`

No real secrets, keys, or tokens are present anywhere in the passing sandbox.

**Expected outcome:** 0 findings → PASS

---

### Flutter Analyze — Clean Dart

**Source:** `flutter_sandbox/lib/main.dart`

Standard Flutter counter app template. No unused variables, no dead code,
no always-true conditions, no bad practices.

**Expected outcome:** 0 findings → PASS

---

### Checkstyle — Clean Java Style

**Source files:**
- `Application.java` — utility class with private constructor, Javadoc
- `User.java` — immutable POJO with full Javadoc on all methods
- `UserService.java` — clean service class with Javadoc

All files include Javadoc, follow Google naming conventions, and have
correct formatting. Should produce zero Checkstyle errors.

**Expected outcome:** 0 findings → PASS

---

### PMD — Clean Java Source

**Source files:** Same as Checkstyle above.

No empty catch blocks, no unused variables, no system print statements,
no empty methods, no pointless operations.

**Expected outcome:** 0 findings → PASS

---

### SpotBugs — Clean Bytecode

**Compiled classes:** `target/classes/com/careconnect/sandbox/`
- `Application.class`
- `User.class`
- `UserService.class`

No SQL concatenation, no insecure RNG, no null dereferences, no resource
leaks, no weak crypto, no broken `equals`/`hashCode`.

**Expected outcome:** 0 findings → PASS

---

### Semgrep — Clean Code

**Source files:** Same Java and Dart files as above.

The passing `.semgrep.yml` defines the same `demo.hardcoded-secret` rule
but none of the passing sandbox files contain `SECRET_...` string patterns.
No `MessageDigest.getInstance("MD5")` calls are present.

**Expected outcome:** 0 findings → PASS

---

### OWASP Dependency-Check — Clean Dependencies

**Source:** `pom.xml`, `pubspec.yaml`

Dependencies declared in the passing sandbox should be current versions
with no known CVEs. Verify that both files pin up-to-date versions before
treating this as a passing test case.

**Expected outcome:** 0 CVEs → PASS

---

### Sonar — Disabled

Same as failing sandbox — disabled, advisory only, never blocks.

**Expected outcome:** Disabled → PASS

---

## Test Validation Checklist

Use this checklist to confirm the gate engine is working correctly
after each deployment or code change.

### Failing sandbox validation

| Check | How to verify |
|-------|---------------|
| Gate exits with code 1 | Workflow step "Run Quality Gate" shows ❌ |
| `overall_block=true` in evaluated.json | Download artifact, inspect evaluated.json |
| TruffleHog findings present | `blocking_results` entry for `trufflehog` has `policy_violation=true` |
| At least one Java tool (Checkstyle/PMD/SpotBugs) has findings | `policy_violation=true` for at least one |
| Semgrep has findings | `policy_violation=true` for `semgrep` |
| report.md shows 🚫 BLOCKED | GitHub Actions Job Summary |
| report.html shows BLOCKED banner in red | Download artifact, open in browser |
| PR comment shows 🚫 BLOCKED | PR comments section |
| Sonar shows ⏸️ DISABLED | report.md and report.html summary table |

### Passing sandbox validation

| Check | How to verify |
|-------|---------------|
| Gate exits with code 0 | Workflow step "Run Quality Gate" shows ✅ |
| `overall_block=false` in evaluated.json | Download artifact, inspect evaluated.json |
| All blocking tools show `policy_violation=false` | `blocking_results` in evaluated.json |
| report.md shows ✅ APPROVED | GitHub Actions Job Summary |
| report.html shows APPROVED banner in green | Download artifact, open in browser |
| PR comment shows ✅ APPROVED | PR comments section |

---

## Known Gaps and Action Items

| # | Issue | Action Required |
|---|-------|----------------|
| 1 | `pom.xml` (failing) may not contain a CVE-bearing dependency | Add `spring-core:5.3.18` or similar outdated dep to trigger Dependency-Check |
| 2 | `pubspec.yaml` (failing) dependency coverage unknown | Verify Dependency-Check supports Dart/pub scanning; add vulnerable dep if supported |
| 3 | Flutter analyzer findings depend on Dart SDK version | Pin Dart SDK in `pubspec.yaml` to ensure consistent analyzer behavior |
| 4 | Semgrep `--config=auto` findings are non-deterministic | Consider adding additional custom rules to `.semgrep.yml` for stable test coverage |
| 5 | Sonar is disabled | Activate once SonarQube/SonarCloud account is set up |