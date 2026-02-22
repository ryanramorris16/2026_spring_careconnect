#!/bin/bash
set -e

REPO="umgc/2026_spring_careconnect"
BASE_BRANCH="team_d"
REPO_ROOT="/Volumes/DevDrive/code/2026_spring_careconnect"
BACKEND_DIR="backend/core/src/main/java/com/careconnect/test"
FRONTEND_DIR="frontend/lib/test_violations"

RED='\033[0;31m'; GREEN='\033[0;32m'; BLUE='\033[0;34m'; NC='\033[0m'

echo -e "${BLUE}=== CareConnect CI/CD Test Branch Creator ===${NC}"

cd "$REPO_ROOT"
GH_USER=$(gh api user --jq '.login' 2>/dev/null || echo "")
[ -z "$GH_USER" ] && echo -e "${RED}ERROR: gh not authenticated${NC}" && exit 1
echo -e "${GREEN}✓ gh: $GH_USER${NC}"

create_branch() {
  git branch -D "$1" 2>/dev/null || true
  git fetch origin team_d --quiet
  git checkout -b "$1" origin/team_d --quiet
  echo -e "${GREEN}✓ Branched: $1${NC}"
}

push_branch() {
  git add -A
  git commit -m "$2" --quiet
  git push origin "$1" --force --quiet
  echo -e "${GREEN}✓ Pushed: $1${NC}"
  git checkout team_d-james-cicd-gating --quiet
}

make_dirs() { mkdir -p "$BACKEND_DIR" "$FRONTEND_DIR"; }
cleanup() { rm -rf "$BACKEND_DIR" "$FRONTEND_DIR"; }

echo -e "${BLUE}════ BRANCH 1: test/all-passing ════${NC}"
create_branch "test/all-passing"
cleanup; make_dirs
cat > "$BACKEND_DIR/CleanService.java" << 'JAVA'
package com.careconnect.test;
/** Clean service for CI testing. */
public class CleanService {
    /** Name. */
    private final String name;
    /** Constructor. @param name service name */
    public CleanService(final String name) { this.name = name; }
    /** Greet. @return greeting */
    public String greet() { return "Hello from " + name; }
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
push_branch "test/all-passing" "test: all-passing — clean code, no violations"

echo -e "${BLUE}════ BRANCH 2: test/all-failing ════${NC}"
create_branch "test/all-failing"
cleanup; make_dirs
cat > "$BACKEND_DIR/BadService.java" << 'JAVA'
package com.careconnect.test;
import java.io.*;
import java.sql.*;
public class BadService {
    public static String AWS_KEY = "AKIAIOSFODNN7EXAMPLE";
    public String x;
    public void doEverything(String input) {
        String unused = "never used";
        String result = null;
        System.out.println(result.length());
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db", "root", "password123");
            Statement stmt = conn.createStatement();
            stmt.execute("SELECT * FROM users WHERE id = " + input);
        } catch (Exception e) { }
        try {
            FileInputStream fis = new FileInputStream("file.txt");
            int data = fis.read();
        } catch (IOException e) { e.printStackTrace(); }
    }
}
JAVA
cat > "$FRONTEND_DIR/bad_widget.dart" << 'DART'
import 'dart:io';
class BadService {
  final String apiKey = 'hardcoded-secret-key-12345';
  void doSomething() {
    var unused1 = 'never used';
    dynamic anything = 'value';
    var userId = Platform.environment['USER_INPUT'];
    var query = 'SELECT * FROM users WHERE id = $userId';
  }
}
DART
push_branch "test/all-failing" "test: all-failing — violations in every tool"

echo -e "${GREEN}Done! Now open PRs manually on GitHub targeting team_d.${NC}"
echo "  test/all-passing → team_d"
echo "  test/all-failing → team_d"
