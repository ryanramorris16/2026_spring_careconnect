#!/bin/bash
# =============================================================================
# CareConnect — CI/CD Workflow Test Branch Creator
# =============================================================================
#
# PURPOSE:
#   Creates and pushes test branches that trigger specific pass/fail scenarios
#   in the GitHub Actions workflow, then opens PRs against team_d for each.
#
# BRANCHES CREATED:
#   test/all-passing          — All tools pass (clean code)
#   test/all-failing          — All tools fail simultaneously
#   test/fail-checkstyle      — Only Checkstyle fails
#   test/fail-spotbugs        — Only SpotBugs fails
#   test/fail-pmd             — Only PMD fails
#   test/fail-semgrep         — Only Semgrep fails
#   test/fail-flutter-analyze — Only flutter analyze fails
#   test/fail-trufflehog      — Only TruffleHog fails (fake secret pattern)
#   test/fail-owasp           — Only OWASP Dependency-Check fails (real CVE dep)
#
# PREREQUISITES:
#   - gh CLI installed and authenticated (gh auth status should show jstevens888)
#   - You are in the repo root: /Volumes/DevDrive/code/2026_spring_careconnect
#   - SSH key configured for GitHub
#
# USAGE:
#   chmod +x create-test-branches.sh
#   ./create-test-branches.sh
#
# WHAT IT DOES TO YOUR GIT CONFIG:
#   1. Temporarily re-enables push URL to origin
#   2. Pushes all test branches
#   3. Restores push URL to DISABLED
#   Your local branches and working branch are never touched.
#
# AFTER RUNNING:
#   - 9 PRs will be opened in your browser targeting team_d
#   - Watch GitHub Actions tab to see each workflow run
#   - After testing, delete test branches:
#       git branch -D test/all-passing test/all-failing ... (etc)
#       gh pr close <number> for each PR
#
# =============================================================================

set -e  # Exit on any error

# ── Configuration ─────────────────────────────────────────────────────────────
REPO="umgc/2026_spring_careconnect"
BASE_BRANCH="team_d"
REPO_ROOT="/Volumes/DevDrive/code/2026_spring_careconnect"
REMOTE_URL="git@github.com:umgc/2026_spring_careconnect.git"
BACKEND_DIR="backend/core/src/main/java/com/careconnect/test"
FRONTEND_DIR="frontend/lib/test_violations"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color


# Run only these scenarios (space-separated). Example: "all-passing all-failing"
RUN_ONLY="${RUN_ONLY:-all-passing all-failing}"
should_run() { [[ " $RUN_ONLY " == *" $1 "* ]]; }


# ── Safety checks ──────────────────────────────────────────────────────────────
echo -e "${BLUE}=== CareConnect CI/CD Test Branch Creator ===${NC}"
echo ""

# Confirm we're in the right place
if [ ! -d "$REPO_ROOT/.git" ]; then
  echo -e "${RED}ERROR: Not in the repo root. Run this from:${NC}"
  echo "  $REPO_ROOT"
  exit 1
fi

cd "$REPO_ROOT"

# Confirm we're NOT on main
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ]; then
  echo -e "${RED}ERROR: You are on main. Switch to your working branch first:${NC}"
  echo "  git checkout team_d-james-cicd-gating"
  exit 1
fi

echo -e "${GREEN}✓ Repo root confirmed${NC}"
echo -e "${GREEN}✓ Current branch: $CURRENT_BRANCH${NC}"
echo ""

# Confirm gh is authenticated
GH_USER=$(gh api user --jq '.login' 2>/dev/null || echo "")
if [ -z "$GH_USER" ]; then
  echo -e "${RED}ERROR: gh CLI not authenticated. Run: gh auth login${NC}"
  exit 1
fi
echo -e "${GREEN}✓ gh CLI authenticated as: $GH_USER${NC}"
echo ""

# Confirm team_d exists on remote
if ! git ls-remote --heads origin team_d | grep -q team_d; then
  echo -e "${RED}ERROR: team_d branch not found on remote origin.${NC}"
  exit 1
fi
echo -e "${GREEN}✓ team_d branch confirmed on remote${NC}"
echo ""

# ── Re-enable push temporarily ────────────────────────────────────────────────
echo -e "${YELLOW}→ Temporarily re-enabling push to origin...${NC}"
git remote set-url --push origin "$REMOTE_URL"
echo -e "${GREEN}✓ Push enabled${NC}"
echo ""

# Ensure push URL is always restored even if script fails
restore_push_disabled() {
  echo ""
  echo -e "${YELLOW}→ Restoring push URL to DISABLED...${NC}"
  git remote set-url --push origin DISABLED
  echo -e "${GREEN}✓ Push URL restored to DISABLED${NC}"
}
trap restore_push_disabled EXIT

# ── Helper functions ───────────────────────────────────────────────────────────

# Create a branch from team_d, add files, commit, push, open PR
create_branch() {
  local BRANCH_NAME="$1"
  local PR_TITLE="$2"
  local PR_BODY="$3"

  echo -e "${BLUE}─── Creating branch: $BRANCH_NAME ───${NC}"

  # Delete local branch if it already exists (clean slate)
  git branch -D "$BRANCH_NAME" 2>/dev/null || true

  # Branch off the latest team_d
  git fetch origin team_d --quiet
  git checkout -b "$BRANCH_NAME" origin/team_d --quiet

  echo -e "${GREEN}✓ Branched from origin/team_d${NC}"
}

commit_and_push() {
  local BRANCH_NAME="$1"
  local COMMIT_MSG="$2"

  git add -A
  git commit -m "$COMMIT_MSG" --quiet
  git push origin "$BRANCH_NAME" --force --quiet
  echo -e "${GREEN}✓ Pushed $BRANCH_NAME to origin${NC}"
}

open_pr() {
  local BRANCH_NAME="$1"
  local PR_TITLE="$2"
  local PR_BODY="$3"

  gh pr create \
    --repo "$REPO" \
    --base "$BASE_BRANCH" \
    --head "$BRANCH_NAME" \
    --title "$PR_TITLE" \
    --body "$PR_BODY" \
    --web  # Opens in browser so you can see it

  echo -e "${GREEN}✓ PR opened for $BRANCH_NAME${NC}"
  echo ""
}

# Create backend test directory structure
make_backend_dirs() {
  mkdir -p "$BACKEND_DIR"
}

# Create frontend test directory structure
make_frontend_dirs() {
  mkdir -p "$FRONTEND_DIR"
}

# Clean up test files before each branch (so files don't carry over)
cleanup_test_files() {
  rm -rf "$BACKEND_DIR" "$FRONTEND_DIR"
}


# =============================================================================
# BRANCH 1: test/all-passing
# Clean, valid Java and Dart code. No violations. No secrets. No bad deps.
# Every tool should report PASSED.
# =============================================================================
if should_run "all-passing"; then
    echo ""
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}  BRANCH 1: test/all-passing            ${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"

    create_branch "test/all-passing" "" ""
    cleanup_test_files
    make_backend_dirs
    make_frontend_dirs

    # Clean Java class — satisfies Checkstyle, SpotBugs, PMD
    cat > "$BACKEND_DIR/CleanService.java" <<- 'JAVA'
    package com.careconnect.test;

    /**
    * A clean service class used for CI/CD pipeline testing.
    * This class intentionally has no violations.
    */
    public class CleanService {

        /** The name of this service instance. */
        private final String name;

        /**
        * Constructs a new CleanService with the given name.
        *
        * @param name the service name
        */
        public CleanService(final String name) {
            this.name = name;
        }

        /**
        * Returns a greeting message.
        *
        * @return greeting string
        */
        public String greet() {
            return "Hello from " + name;
        }
    }
    JAVA

    # Clean Dart file — satisfies flutter analyze and Semgrep
    cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
    // Clean Dart file for CI/CD pipeline testing.
    // No violations intentional.

    /// A simple clean service for testing purposes.
    class CleanService {
    final String name;

    /// Creates a [CleanService] with the given [name].
    const CleanService({required this.name});

    /// Returns a greeting string.
    String greet() => 'Hello from $name';
    }
    DART

    commit_and_push "test/all-passing" "test: all-passing scenario — clean code, no violations"

    open_pr "test/all-passing" \
    "[CI TEST] All Tools Passing" \
    "## CI/CD Pipeline Test — All Passing

    This PR contains intentionally clean code designed to make every analysis tool pass.

    **Expected results:**
    - ✅ TruffleHog — no secrets
    - ✅ Checkstyle — no style violations
    - ✅ SpotBugs — no bug patterns
    - ✅ PMD — no source issues
    - ✅ Semgrep — no SAST findings
    - ✅ flutter analyze — no Dart issues
    - ✅ OWASP Dependency-Check — no CVEs (uses existing deps)

    **Do not merge — test branch only.**"
fi

# =============================================================================
# BRANCH 2: test/all-failing
# Deliberately bad code that triggers every tool simultaneously.
# =============================================================================
if should_run "all-failing"; then

    echo ""
    echo -e "${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}  BRANCH 2: test/all-failing            ${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}"

    create_branch "test/all-failing" "" ""
    cleanup_test_files
    make_backend_dirs
    make_frontend_dirs

    # Bad Java — triggers Checkstyle (missing Javadoc, bad naming),
    # SpotBugs (null dereference, resource leak),
    # PMD (empty catch, unused variable, too complex)
    cat > "$BACKEND_DIR/BadService.java" << 'JAVA'
    package com.careconnect.test;
    import java.io.*;
    import java.sql.*;

    public class BadService {
        public static String API_KEY = "AKIAIOSFODNN7EXAMPLE";
        public String x;
        public String y;
        public String z;

        public void doEverything(String input) {
            String unused = "never used";
            String result = null;
            System.out.println(result.length());

            try {
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "root", "password123");
                Statement stmt = conn.createStatement();
                stmt.execute("SELECT * FROM users WHERE id = " + input);
            } catch (Exception e) {
            }

            try {
                FileInputStream fis = new FileInputStream("file.txt");
                int data = fis.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (input != null) {
                if (input.length() > 0) {
                    if (input.startsWith("a")) {
                        if (input.endsWith("z")) {
                            if (input.contains("m")) {
                                System.out.println("deep nesting violation");
                            }
                        }
                    }
                }
            }
        }

        public void doEverything2(String a) { doEverything(a); }
        public void doEverything3(String a) { doEverything(a); }
        public void doEverything4(String a) { doEverything(a); }
        public void doEverything5(String a) { doEverything(a); }
        public void doEverything6(String a) { doEverything(a); }
    }
    JAVA

    # Bad Dart — triggers flutter analyze (type errors, unused vars)
    # and Semgrep (hardcoded credentials pattern)
    cat > "$FRONTEND_DIR/bad_widget.dart" << 'DART'
    // ignore_for_file: unused_local_variable
    import 'dart:io';

    class BadService {
    // Hardcoded credential — triggers Semgrep
    final String apiKey = 'hardcoded-secret-key-12345';
    final String password = 'supersecretpassword';

    void doSomething() {
        // Unused variables — triggers flutter analyze
        var unused1 = 'never used';
        int unused2 = 42;

        // Unsafe dynamic usage
        dynamic anything = 'could be anything';
        print(anything.nonExistentMethod());

        // Hardcoded IP — triggers Semgrep
        var serverUrl = 'http://192.168.1.100:8080/api';

        // SQL injection pattern — triggers Semgrep
        var userId = Platform.environment['USER_INPUT'];
        var query = 'SELECT * FROM users WHERE id = $userId';
    }
    }
    DART

    # Add a vulnerable dependency to pom.xml for OWASP — Log4Shell (CVE-2021-44228, CVSS 10.0)
    # We append to the existing pom.xml rather than replacing it
    # NOTE: After testing, remove this dependency. It is intentionally vulnerable.
    if [ -f "backend/core/pom.xml" ]; then
    # Insert vulnerable log4j before </dependencies>
    sed -i.bak 's|</dependencies>|        <!-- TEST ONLY: Vulnerable log4j — CVE-2021-44228 (Log4Shell) CVSS 10.0 -->\n        <!-- REMOVE AFTER TESTING -->\n        <dependency>\n            <groupId>org.apache.logging.log4j</groupId>\n            <artifactId>log4j-core</artifactId>\n            <version>2.14.1</version>\n        </dependency>\n    </dependencies>|' backend/core/pom.xml
    fi

    # Fake AWS secret in a config file — triggers TruffleHog
    # Format matches TruffleHog's AWS detector pattern
    # NOTE: --only-verified is set, so TruffleHog detects but may not hard-fail
    # unless the key is active. This demonstrates detection capability.
    cat > "$BACKEND_DIR/TestConfig.java" << 'JAVA'
    package com.careconnect.test;

    // TEST ONLY — fake credentials to trigger TruffleHog detection
    // These are not real credentials. Remove after testing.
    public class TestConfig {
        // Fake AWS Access Key — matches TruffleHog detector pattern
        private static final String AWS_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
        private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    }
    JAVA

    commit_and_push "test/all-failing" "test: all-failing scenario — intentional violations in every tool"

    # Restore pom.xml backup if we modified it
    if [ -f "backend/core/pom.xml.bak" ]; then
    mv backend/core/pom.xml.bak backend/core/pom.xml
    fi

    open_pr "test/all-failing" \
    "[CI TEST] All Tools Failing" \
    "## CI/CD Pipeline Test — All Failing

    This PR contains intentionally bad code designed to trigger every analysis tool.

    **Expected results:**
    - ❌ TruffleHog — fake AWS key pattern detected
    - ❌ Checkstyle — missing Javadoc, bad naming, style violations
    - ❌ SpotBugs — null dereference, SQL injection, resource leak
    - ❌ PMD — empty catch block, unused variables, deep nesting
    - ❌ Semgrep — hardcoded credentials, SQL injection, hardcoded IP
    - ❌ flutter analyze — unused variables, unsafe dynamic usage
    - ❌ OWASP Dependency-Check — Log4Shell CVE-2021-44228 (CVSS 10.0)

    **Merge should be BLOCKED by quality gates.**
    **Do not merge — test branch only.**"
fi

# =============================================================================
# BRANCH 3: test/fail-checkstyle
# Only Checkstyle violations. Everything else clean.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 3: test/fail-checkstyle        ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-checkstyle" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

cat > "$BACKEND_DIR/CheckstyleViolation.java" << 'JAVA'
package com.careconnect.test;
// Checkstyle violations:
//   - Missing Javadoc on class
//   - Missing Javadoc on method
//   - Line too long (exceeds 100/120 char limit typically configured)
//   - Method name doesn't follow camelCase (starts uppercase)
//   - Magic number used directly
public class CheckstyleViolation {
    public void BadMethodName() {
        int x = 12345678;
        System.out.println(x);
    }
    public void anotherBadMethodName() { int y = 99999999; System.out.println("This line is intentionally very long to trigger the line length checkstyle rule which is usually set to 100 or 120 characters"); }
}
JAVA

# Clean Dart — no flutter analyze or Semgrep violations
cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
/// A clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => 'Hello from $name';
}
DART

commit_and_push "test/fail-checkstyle" "test: only Checkstyle violations — missing Javadoc, bad naming, line length"

open_pr "test/fail-checkstyle" \
  "[CI TEST] Checkstyle Fails Only" \
  "## CI/CD Pipeline Test — Checkstyle Only

**Expected results:**
- ✅ TruffleHog — passes
- ❌ Checkstyle — missing Javadoc, bad method naming, line too long
- ✅ SpotBugs — passes
- ✅ PMD — passes
- ✅ Semgrep — passes
- ✅ flutter analyze — passes
- ✅ OWASP Dependency-Check — passes

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 4: test/fail-spotbugs
# Only SpotBugs violations. Everything else clean.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 4: test/fail-spotbugs          ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-spotbugs" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

cat > "$BACKEND_DIR/SpotBugsViolation.java" << 'JAVA'
package com.careconnect.test;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Demonstrates SpotBugs violations for CI/CD pipeline testing.
 * Violations: null dereference, resource leak, ignored return value.
 */
public class SpotBugsViolation {

    /**
     * Triggers null dereference (NP_NULL_ON_SOME_PATH)
     * and resource leak (OBJ_OPEN_STREAM_EXCEPTION_PATH).
     *
     * @param input input string
     */
    public void riskyMethod(final String input) {
        // SpotBugs: NP_NULL_ON_SOME_PATH — calling method on potentially null object
        String value = null;
        if (input.equals("trigger")) {
            value = "set";
        }
        // value could be null here
        System.out.println(value.length());

        // SpotBugs: OBJ_OPEN_STREAM_EXCEPTION_PATH — stream not closed in finally block
        try {
            FileInputStream fis = new FileInputStream("test.txt");
            int data = fis.read();
            System.out.println(data);
            // fis is never closed — resource leak
        } catch (IOException e) {
            throw new RuntimeException("Failed to read", e);
        }
    }

    /**
     * Triggers ignored return value (RV_RETURN_VALUE_IGNORED).
     *
     * @param str the string to process
     */
    public void ignoredReturn(final String str) {
        // SpotBugs: RV_RETURN_VALUE_IGNORED — String.replace return value ignored
        str.replace("old", "new");
    }
}
JAVA

cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
/// Clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => 'Hello from $name';
}
DART

commit_and_push "test/fail-spotbugs" "test: only SpotBugs violations — null dereference, resource leak"

open_pr "test/fail-spotbugs" \
  "[CI TEST] SpotBugs Fails Only" \
  "## CI/CD Pipeline Test — SpotBugs Only

**Expected results:**
- ✅ TruffleHog — passes
- ✅ Checkstyle — passes
- ❌ SpotBugs — null dereference, resource leak, ignored return value
- ✅ PMD — passes
- ✅ Semgrep — passes
- ✅ flutter analyze — passes
- ✅ OWASP Dependency-Check — passes

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 5: test/fail-pmd
# Only PMD violations. Everything else clean.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 5: test/fail-pmd               ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-pmd" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

cat > "$BACKEND_DIR/PmdViolation.java" << 'JAVA'
package com.careconnect.test;

/**
 * Demonstrates PMD violations for CI/CD pipeline testing.
 * Violations: empty catch block, unused variable, overly complex method.
 */
public class PmdViolation {

    /**
     * Triggers PMD empty catch block rule (EmptyCatchBlock).
     *
     * @param input input string
     */
    public void emptyCatch(final String input) {
        try {
            int value = Integer.parseInt(input);
            System.out.println(value);
        } catch (NumberFormatException e) {
            // PMD: EmptyCatchBlock — catching exception and doing nothing
        }
    }

    /**
     * Triggers PMD unused variable (UnusedLocalVariable)
     * and avoid catching generic exception (AvoidCatchingGenericException).
     *
     * @param data the data to process
     */
    public void unusedVariable(final String data) {
        // PMD: UnusedLocalVariable
        String unused = "this value is never read";
        int anotherUnused = 42;

        try {
            System.out.println(data.toUpperCase());
        } catch (Exception e) {
            // PMD: AvoidCatchingGenericException
            System.out.println("error");
        }
    }

    /**
     * Triggers PMD cyclomatic complexity rule (CyclomaticComplexity).
     *
     * @param a first value
     * @param b second value
     * @param c third value
     * @param d fourth value
     * @return computed result
     */
    public int tooComplex(
            final int a, final int b, final int c, final int d) {
        // PMD: CyclomaticComplexity — too many branches
        if (a > 0) {
            if (b > 0) {
                if (c > 0) {
                    if (d > 0) {
                        return 1;
                    } else if (d < -10) {
                        return 2;
                    } else {
                        return 3;
                    }
                } else if (c < -10) {
                    return 4;
                } else {
                    return 5;
                }
            } else if (b < -10) {
                return 6;
            } else {
                return 7;
            }
        } else if (a < -10) {
            return 8;
        } else {
            return 9;
        }
    }
}
JAVA

cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
/// Clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => 'Hello from $name';
}
DART

commit_and_push "test/fail-pmd" "test: only PMD violations — empty catch, unused vars, high complexity"

open_pr "test/fail-pmd" \
  "[CI TEST] PMD Fails Only" \
  "## CI/CD Pipeline Test — PMD Only

**Expected results:**
- ✅ TruffleHog — passes
- ✅ Checkstyle — passes
- ✅ SpotBugs — passes
- ❌ PMD — empty catch block, unused variables, cyclomatic complexity
- ✅ Semgrep — passes
- ✅ flutter analyze — passes
- ✅ OWASP Dependency-Check — passes

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 6: test/fail-semgrep
# Only Semgrep violations. Everything else clean.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 6: test/fail-semgrep           ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-semgrep" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

# Clean Java (passes Checkstyle/SpotBugs/PMD) but has patterns Semgrep catches
cat > "$BACKEND_DIR/SemgrepViolation.java" << 'JAVA'
package com.careconnect.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Demonstrates Semgrep SAST violations for CI/CD pipeline testing.
 * Violations: SQL injection, hardcoded credentials, weak crypto.
 */
public class SemgrepViolation {

    /** Hardcoded database password — Semgrep: hardcoded-credentials. */
    private static final String DB_PASSWORD = "hardcoded_db_password_123";

    /**
     * Triggers Semgrep SQL injection rule.
     * User input is concatenated directly into SQL query.
     *
     * @param userId raw user input
     * @throws Exception on database error
     */
    public void sqlInjection(final String userId) throws Exception {
        // Semgrep: tainted-sql-string — user input in SQL query
        Connection conn = DriverManager.getConnection(
            "jdbc:mysql://localhost/careconnect", "root", DB_PASSWORD);
        Statement stmt = conn.createStatement();
        // VULNERABILITY: direct string concatenation with user input
        stmt.execute("SELECT * FROM patients WHERE id = " + userId);
        conn.close();
    }

    /**
     * Triggers Semgrep weak hashing rule.
     *
     * @param data data to hash
     * @return MD5 hash (weak)
     * @throws Exception on hash error
     */
    public byte[] weakHash(final String data) throws Exception {
        // Semgrep: use-of-md5 — MD5 is cryptographically weak
        java.security.MessageDigest md =
            java.security.MessageDigest.getInstance("MD5");
        return md.digest(data.getBytes());
    }
}
JAVA

# Dart file with Semgrep violations
cat > "$FRONTEND_DIR/semgrep_violation.dart" << 'DART'
/// Dart file with Semgrep violations for CI/CD testing.
class SemgrepViolation {
  // Semgrep: hardcoded credentials in Dart
  final String apiSecret = 'hardcoded-api-secret-key-abc123';

  /// Builds a URL with a hardcoded internal IP — Semgrep flags this.
  String getEndpoint() {
    // Semgrep: hardcoded-ip-address
    return 'http://10.0.0.1:8080/api/patients';
  }
}
DART

commit_and_push "test/fail-semgrep" "test: only Semgrep violations — SQL injection, hardcoded credentials, weak hash"

open_pr "test/fail-semgrep" \
  "[CI TEST] Semgrep Fails Only" \
  "## CI/CD Pipeline Test — Semgrep Only

**Expected results:**
- ✅ TruffleHog — passes (no git-committed secrets)
- ✅ Checkstyle — passes
- ✅ SpotBugs — passes
- ✅ PMD — passes
- ❌ Semgrep — SQL injection, hardcoded credentials, MD5 weak hash
- ✅ flutter analyze — passes
- ✅ OWASP Dependency-Check — passes

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 7: test/fail-flutter-analyze
# Only flutter analyze violations. Everything else clean.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 7: test/fail-flutter-analyze   ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-flutter-analyze" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

# Clean Java
cat > "$BACKEND_DIR/CleanService.java" << 'JAVA'
package com.careconnect.test;

/**
 * Clean Java class for CI/CD pipeline testing.
 * No violations.
 */
public class CleanService {

    /** Service name. */
    private final String name;

    /**
     * Constructs a CleanService.
     *
     * @param name the service name
     */
    public CleanService(final String name) {
        this.name = name;
    }

    /**
     * Returns a greeting.
     *
     * @return greeting string
     */
    public String greet() {
        return "Hello from " + name;
    }
}
JAVA

# Dart with flutter analyze violations
# These are real lint errors that analysis_options.yaml will catch
cat > "$FRONTEND_DIR/flutter_analyze_violation.dart" << 'DART'
// Flutter analyze violations for CI/CD pipeline testing.
// Violations: unused imports, unnecessary type annotations,
// prefer const constructors, avoid print statements.

import 'dart:io';        // unused import
import 'dart:convert';   // unused import

class FlutterAnalyzeViolation {
  String name;           // should be final

  FlutterAnalyzeViolation(this.name);

  void doSomething() {
    // flutter analyze: avoid_print
    print('This triggers avoid_print lint rule');

    // flutter analyze: unused_local_variable
    String unused = 'never read';

    // flutter analyze: unnecessary_type_check (always true)
    if (name is String) {
      print(name);
    }

    // flutter analyze: prefer_single_quotes (if configured)
    String doubleQuoted = "should use single quotes";
    print(doubleQuoted);
  }
}
DART

commit_and_push "test/fail-flutter-analyze" "test: only flutter analyze violations — unused imports, avoid_print, unused vars"

open_pr "test/fail-flutter-analyze" \
  "[CI TEST] Flutter Analyze Fails Only" \
  "## CI/CD Pipeline Test — Flutter Analyze Only

**Expected results:**
- ✅ TruffleHog — passes
- ✅ Checkstyle — passes
- ✅ SpotBugs — passes
- ✅ PMD — passes
- ✅ Semgrep — passes
- ❌ flutter analyze — unused imports, avoid_print, unused variables
- ✅ OWASP Dependency-Check — passes

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 8: test/fail-trufflehog
# Only TruffleHog detection. Everything else clean.
#
# NOTE ON --only-verified:
#   Your workflow uses --only-verified, meaning TruffleHog will detect the
#   fake key pattern but will NOT hard-fail unless the key is verified active
#   against AWS. This branch demonstrates detection capability.
#   To guarantee a hard failure, temporarily remove --only-verified from
#   the workflow's TruffleHog step extra_args.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 8: test/fail-trufflehog        ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-trufflehog" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

# Clean Java
cat > "$BACKEND_DIR/CleanService.java" << 'JAVA'
package com.careconnect.test;

/**
 * Clean Java class. No violations.
 */
public class CleanService {

    /** Service name. */
    private final String name;

    /**
     * Constructs a CleanService.
     *
     * @param name the service name
     */
    public CleanService(final String name) {
        this.name = name;
    }

    /**
     * Returns a greeting.
     *
     * @return greeting string
     */
    public String greet() {
        return "Hello from " + name;
    }
}
JAVA

# Clean Dart
cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
/// Clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => 'Hello from $name';
}
DART

# File with fake secret patterns — TruffleHog detects these
# Format exactly matches TruffleHog's built-in detectors
cat > "$BACKEND_DIR/LegacyConfig.java" << 'JAVA'
package com.careconnect.test;

/**
 * Legacy configuration class committed with fake credentials.
 * Intentionally triggers TruffleHog for CI/CD pipeline testing.
 * DO NOT use patterns like this in real code.
 */
public class LegacyConfig {

    // TruffleHog detector: AWS Access Key ID pattern
    // Format: AKIA[0-9A-Z]{16}
    private static final String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";

    // TruffleHog detector: AWS Secret Access Key pattern
    private static final String AWS_SECRET = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    // TruffleHog detector: Generic API key pattern
    private static final String STRIPE_KEY = "STRIPE_KEY_REDACTED_FOR_TESTING";

    /**
     * Returns the configured AWS key (insecure — for test only).
     *
     * @return the AWS key string
     */
    public String getAwsKey() {
        return AWS_KEY;
    }
}
JAVA

commit_and_push "test/fail-trufflehog" "test: TruffleHog detection — fake AWS key and API key patterns committed"

open_pr "test/fail-trufflehog" \
  "[CI TEST] TruffleHog Fails Only" \
  "## CI/CD Pipeline Test — TruffleHog Only

**Expected results:**
- ❌ TruffleHog — fake AWS key and API key patterns detected in committed code
- ✅ Checkstyle — passes
- ✅ SpotBugs — passes
- ✅ PMD — passes
- ✅ Semgrep — passes
- ✅ flutter analyze — passes
- ✅ OWASP Dependency-Check — passes

> ⚠️ **Note:** Workflow uses \`--only-verified\`. TruffleHog will detect the patterns
> but may not hard-fail since these are fake (inactive) keys. To guarantee failure,
> temporarily remove \`--only-verified\` from the workflow's TruffleHog \`extra_args\`.

**Do not merge — test branch only.**"


# =============================================================================
# BRANCH 9: test/fail-owasp
# Only OWASP Dependency-Check fails. Everything else clean.
# Uses log4j 2.14.1 which has CVE-2021-44228 (Log4Shell, CVSS 10.0).
# REMOVE the vulnerable dependency from pom.xml after testing.
# =============================================================================
echo ""
echo -e "${BLUE}════════════════════════════════════════${NC}"
echo -e "${BLUE}  BRANCH 9: test/fail-owasp             ${NC}"
echo -e "${BLUE}════════════════════════════════════════${NC}"

create_branch "test/fail-owasp" "" ""
cleanup_test_files
make_backend_dirs
make_frontend_dirs

# Clean Java
cat > "$BACKEND_DIR/CleanService.java" << 'JAVA'
package com.careconnect.test;

/**
 * Clean Java class. No violations.
 */
public class CleanService {

    /** Service name. */
    private final String name;

    /**
     * Constructs a CleanService.
     *
     * @param name the service name
     */
    public CleanService(final String name) {
        this.name = name;
    }

    /**
     * Returns a greeting.
     *
     * @return greeting string
     */
    public String greet() {
        return "Hello from " + name;
    }
}
JAVA

# Clean Dart
cat > "$FRONTEND_DIR/clean_widget.dart" << 'DART'
/// Clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => 'Hello from $name';
}
DART

# Add vulnerable log4j to pom.xml
# CVE-2021-44228 (Log4Shell) — CVSS 10.0 Critical
# This will be caught by OWASP Dependency-Check and hard-block the merge
if [ -f "backend/core/pom.xml" ]; then
  sed -i.bak 's|</dependencies>|        <!--\n            TEST ONLY: Vulnerable log4j dependency\n            CVE-2021-44228 (Log4Shell) — CVSS 10.0 Critical\n            REMOVE THIS BLOCK AFTER TESTING\n        -->\n        <dependency>\n            <groupId>org.apache.logging.log4j</groupId>\n            <artifactId>log4j-core</artifactId>\n            <version>2.14.1</version>\n        </dependency>\n    </dependencies>|' backend/core/pom.xml
fi

commit_and_push "test/fail-owasp" "test: only OWASP failure — log4j 2.14.1 CVE-2021-44228 (Log4Shell CVSS 10.0)"

# Restore pom.xml
if [ -f "backend/core/pom.xml.bak" ]; then
  mv backend/core/pom.xml.bak backend/core/pom.xml
fi

open_pr "test/fail-owasp" \
  "[CI TEST] OWASP Dependency-Check Fails Only" \
  "## CI/CD Pipeline Test — OWASP Dependency-Check Only

**Expected results:**
- ✅ TruffleHog — passes
- ✅ Checkstyle — passes
- ✅ SpotBugs — passes
- ✅ PMD — passes
- ✅ Semgrep — passes
- ✅ flutter analyze — passes
- ❌ OWASP Dependency-Check — log4j 2.14.1 / CVE-2021-44228 (Log4Shell) CVSS 10.0 — **HARD BLOCK**

> ⚠️ **Note:** This adds a real vulnerable dependency (log4j 2.14.1) to pom.xml.
> It is removed from your local files immediately after the branch is pushed.
> The branch itself contains the vulnerable dep — do not merge it.

**Do not merge — test branch only.**"


# =============================================================================
# DONE
# =============================================================================
echo ""
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  All 9 test branches created and PRs opened!           ${NC}"
echo -e "${GREEN}════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Branches created:${NC}"
echo "  test/all-passing"
echo "  test/all-failing"
echo "  test/fail-checkstyle"
echo "  test/fail-spotbugs"
echo "  test/fail-pmd"
echo "  test/fail-semgrep"
echo "  test/fail-flutter-analyze"
echo "  test/fail-trufflehog"
echo "  test/fail-owasp"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "  1. Go to GitHub Actions tab to watch the workflow runs"
echo "  2. Check each PR for the report comment"
echo "  3. Download artifact bundles from the Actions tab"
echo ""
echo -e "${YELLOW}To clean up test branches when done:${NC}"
echo "  git branch -D test/all-passing test/all-failing test/fail-checkstyle \\"
echo "    test/fail-spotbugs test/fail-pmd test/fail-semgrep \\"
echo "    test/fail-flutter-analyze test/fail-trufflehog test/fail-owasp"
echo ""
echo "  # Close PRs on GitHub or via gh:"
echo "  gh pr list --repo umgc/2026_spring_careconnect | grep '\[CI TEST\]'"
echo ""
echo -e "${YELLOW}Remember:${NC} Also update your workflow triggers to include team_d:"
echo "  on:"
echo "    push:"
echo "      branches: [main, develop, team_d]"
echo "    pull_request:"
echo "      branches: [main, develop, team_d]"
echo ""