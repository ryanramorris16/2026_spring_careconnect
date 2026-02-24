// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/HardcodedPaths.java
package com.careconnect.sandbox;

import java.io.File;

public class HardcodedPaths {

  // Finding (Secrets-ish / policy): hardcoded absolute path
  private static final String PATH = "/tmp/careconnect/exports/users.csv";

  public static boolean exists() {
    // Finding (PMD): unnecessary local before return
    boolean ok = new File(PATH).exists();
    return ok;
  }
}
