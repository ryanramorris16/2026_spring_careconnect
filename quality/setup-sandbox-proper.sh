#!/usr/bin/env bash
set -euo pipefail

ROOT="quality/sandbox"
PASS="$ROOT/passing"
FAIL="$ROOT/failing"

echo "Creating proper sandbox fixtures under: $ROOT"
rm -rf "$ROOT"
mkdir -p "$PASS" "$FAIL"

# ------------------------------------------------------------
# Common: deterministic Semgrep rules (so auto rules don't drift)
# We'll run semgrep using this config: semgrep --config "$SCAN_ROOT/.semgrep.yml"
# ------------------------------------------------------------
cat > "$PASS/.semgrep.yml" <<'YML'
rules:
  - id: demo.hardcoded-secret
    patterns:
      - pattern: $X = "SECRET_..."
    message: Hardcoded secret-like string (demo rule)
    severity: ERROR
    languages: [python, javascript, java, dart]
YML

cat > "$FAIL/.semgrep.yml" <<'YML'
rules:
  - id: demo.hardcoded-secret
    patterns:
      - pattern: $X = "SECRET_..."
    message: Hardcoded secret-like string (demo rule)
    severity: ERROR
    languages: [python, javascript, java, dart]

  - id: demo.insecure-hash
    pattern: MessageDigest.getInstance("MD5")
    message: Insecure hash algorithm MD5 (demo rule)
    severity: ERROR
    languages: [java]
YML

# ------------------------------------------------------------
# TruffleHog fixtures
# - Passing: no secrets
# - Failing: contains 2 "secret-like" tokens TruffleHog will likely flag
# ------------------------------------------------------------
mkdir -p "$PASS/secrets" "$FAIL/secrets"

cat > "$PASS/secrets/README.txt" <<'TXT'
This folder intentionally contains no secrets.
TXT

cat > "$FAIL/secrets/secret.txt" <<'TXT'
# Demo secrets for TruffleHog sandbox testing ONLY (DO NOT COPY TO REAL CODE)
AWS_ACCESS_KEY_ID=AKIA1234567890ABCDE1
AWS_SECRET_ACCESS_KEY=abcdEFGHijklMNOPqrstUVWXyz0123456789ABCD
TXT

# ------------------------------------------------------------
# Java: minimal Maven project (Dependency-Check reads pom.xml nicely)
# We'll also produce Checkstyle/PMD/SpotBugs findings from src code.
# ------------------------------------------------------------
make_java_project () {
  local BASE="$1"
  local MODE="$2" # passing|failing

  mkdir -p "$BASE/java-sandbox/src/main/java/sandbox"
  mkdir -p "$BASE/java-sandbox/src/test/java/sandbox"

  cat > "$BASE/java-sandbox/pom.xml" <<'XML'
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>sandbox</groupId>
  <artifactId>java-sandbox</artifactId>
  <version>1.0.0</version>

  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>

  <dependencies>
    <!-- PASSING will keep dependencies empty (0 findings). -->
  </dependencies>
</project>
XML

  if [[ "$MODE" == "failing" ]]; then
    # Two commonly-flagged vulnerable deps; Dependency-Check should find >0 CVEs.
    # (Exact CVE count can vary as NVD updates, but this is usually reliably non-zero.)
    perl -0777 -i -pe 's|</dependencies>|  <dependency>\n    <groupId>org.apache.logging.log4j</groupId>\n    <artifactId>log4j-core</artifactId>\n    <version>2.14.1</version>\n  </dependency>\n  <dependency>\n    <groupId>com.fasterxml.jackson.core</groupId>\n    <artifactId>jackson-databind</artifactId>\n    <version>2.9.10.7</version>\n  </dependency>\n</dependencies>|s' "$BASE/java-sandbox/pom.xml"
  fi

  # Passing Java: clean simple code.
  if [[ "$MODE" == "passing" ]]; then
    cat > "$BASE/java-sandbox/src/main/java/sandbox/Ok.java" <<'JAVA'
package sandbox;

public class Ok {
  public static int add(int a, int b) {
    return a + b;
  }

  public static void main(String[] args) {
    System.out.println(add(1, 2));
  }
}
JAVA
  else
    # Failing Java: designed to trigger multiple tools:
    # - Checkstyle (style, naming, braces, etc. via google_checks.xml)
    # - PMD best practices (empty catch, unused variable)
    # - Semgrep demo rules (SECRET_..., MD5)
    # - SpotBugs (null deref risk / bad pattern; depends on class analysis)
    cat > "$BASE/java-sandbox/src/main/java/sandbox/Bad.java" <<'JAVA'
package sandbox;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Bad {

  // Semgrep demo rule #1: hardcoded secret-like token
  private static final String apiKey = "SECRET_DEMO_KEY_SHOULD_NOT_BE_HARDCODED";

  public static String md5(String input) {
    try {
      // Semgrep demo rule #2: insecure hash
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] out = md.digest(input.getBytes());
      return new String(out);
    } catch (NoSuchAlgorithmException e) {
      // PMD best practices: empty catch block (should report)
    }
    // SpotBugs/PMD: returning null is a bad pattern, downstream NPE risk
    return null;
  }

  public static void main(String[] args) {
    // PMD: unused local variable
    int unused = 42;

    // Potential NPE usage pattern
    String x = md5("hello");
    System.out.println(x.length());
  }
}
JAVA
  fi
}

make_java_project "$PASS" "passing"
make_java_project "$FAIL" "failing"

# ------------------------------------------------------------
# Dart: proper Flutter project
# - Passing: no issues
# - Failing: analyzer errors + semgrep demo rule (SECRET_...)
# ------------------------------------------------------------
make_flutter_project () {
  local BASE="$1"
  local MODE="$2"

  mkdir -p "$BASE/flutter-sandbox"

  # We store "lib/" files only; workflow will run flutter create in CI and copy these in.
  mkdir -p "$BASE/flutter-sandbox/lib"

  if [[ "$MODE" == "passing" ]]; then
    cat > "$BASE/flutter-sandbox/lib/main.dart" <<'DART'
import 'package:flutter/material.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(home: Scaffold(body: Center(child: Text('OK'))));
  }
}
DART
  else
    cat > "$BASE/flutter-sandbox/lib/main.dart" <<'DART'
import 'package:flutter/material.dart';

void main() => runApp(const MyApp());

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // Semgrep demo rule: SECRET_...
  final String token = "SECRET_DEMO_FLUTTER_TOKEN";

  @override
  Widget build(BuildContext context) {
    // Analyzer issues:
    // 1) unreachable code
    return const MaterialApp(home: Scaffold(body: Center(child: Text('FAIL'))));
    // 2) unused expression (unreachable anyway) + dead code
    // ignore: dead_code
    token;
  }
}
DART
  fi
}

make_flutter_project "$PASS" "passing"
make_flutter_project "$FAIL" "failing"

echo ""
echo "DONE ✅"
echo "Created:"
echo "  - $PASS (clean)"
echo "  - $FAIL (intentionally dirty)"
echo ""
echo "Next: set SCAN_ROOT in build-and-analyze1.yml to one of:"
echo "  SCAN_ROOT: quality/sandbox/passing"
echo "  SCAN_ROOT: quality/sandbox/failing"
