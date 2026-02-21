#!/bin/bash
set -e

REPO="umgc/2026_spring_careconnect"
BASE_BRANCH="team_d"
REPO_ROOT="/Volumes/DevDrive/code/2026_spring_careconnect"
BACKEND_DIR="backend/core/src/main/java/com/careconnect/test"
FRONTEND_DIR="frontend/lib/test_violations"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=== CareConnect CI/CD Test Branch Creator ===${NC}"

if [ ! -d "$REPO_ROOT/.git" ]; then
  echo -e "${RED}ERROR: Not in the repo root.${NC}"; exit 1
fi

cd "$REPO_ROOT"

CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" = "main" ]; then
  echo -e "${RED}ERROR: You are on main.${NC}"; exit 1
fi

GH_USER=$(gh api user --jq '.login' 2>/dev/null || echo "")
if [ -z "$GH_USER" ]; then
  echo -e "${RED}ERROR: gh CLI not authenticated.${NC}"; exit 1
fi

echo -e "${GREEN}✓ Repo confirmed | Branch: $CURRENT_BRANCH | gh: $GH_USER${NC}"

create_branch() {
  local BRANCH_NAME="$1"
  echo -e "${BLUE}─── Creating: $BRANCH_NAME ───${NC}"
  git branch -D "$BRANCH_NAME" 2>/dev/null || true
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
  echo -e "${GREEN}✓ Pushed $BRANCH_NAME${NC}"
  git checkout team_d-james-cicd-gating --quiet
}

open_pr() {
  local BRANCH_NAME="$1"
  local PR_TITLE="$2"
  local PR_BODY="$3"
  gh pr create --repo "$REPO" --base "$BASE_BRANCH" \
    --head "$BRANCH_NAME" --title "$PR_TITLE" --body "$PR_BODY"
  echo -e "${GREEN}✓ PR created for $BRANCH_NAME${NC}"
}

make_dirs() { mkdir -p "$BACKEND_DIR" "$FRONTEND_DIR"; }
cleanup() { rm -rf "$BACKEND_DIR" "$FRONTEND_DIR"; }

CLEAN_JAVA='package com.careconnect.test;
/**
 * Clean service class for CI/CD pipeline testing.
 */
public class CleanService {
    /** Service name. */
    private final String name;
    /**
     * Constructs a CleanService.
     * @param name the service name
     */
    public CleanService(final String name) { this.name = name; }
    /**
     * Returns a greeting.
     * @return greeting string
     */
    public String greet() { return "Hello from " + name; }
}'

CLEAN_DART='/// Clean Dart file. No violations.
class CleanService {
  final String name;
  const CleanService({required this.name});
  String greet() => '"'"'Hello from $name'"'"';
}'
