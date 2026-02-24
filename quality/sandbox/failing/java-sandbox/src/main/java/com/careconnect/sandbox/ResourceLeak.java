// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/ResourceLeak.java
package com.careconnect.sandbox;

import java.io.BufferedReader;
import java.io.FileReader;

public class ResourceLeak {

  public static String firstLine(String path) throws Exception {
    // Finding (SpotBugs): resource not closed
    BufferedReader br = new BufferedReader(new FileReader(path));
    return br.readLine();
  }

  public static void noop() {
    // Finding (PMD): empty method (intentional)
  }
}
