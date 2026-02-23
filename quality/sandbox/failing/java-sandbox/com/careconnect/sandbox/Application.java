package com.careconnect.sandbox;

public class Application {

  // Finding #1 — hardcoded credential (Semgrep / secret scanners)
  private static final String DB_PASSWORD = "Password123!";

  // Finding #2 — API key-like pattern (Semgrep / TruffleHog)
  private static final String API_KEY = "AKIA1234567890ABCDEF";

  // Finding #3 — additional secret-style constant
  private static final String PASSWORD = "CaregiverDoctor410!";

  public static void main(String[] args) {

    // Finding #4 — guaranteed NullPointerException (SpotBugs)
    String s = null;
    System.out.println(s.length());

    // Finding #5 — empty if block (PMD)
    if (args.length > 0) {
    }

    // Finding #6 — useless self comparison (PMD / Checkstyle)
    if (args == args) {
      System.out.println("self compare");
    }

    // Finding #7 — dead store / unused local (PMD)
    int unused = 42;
  }
}
