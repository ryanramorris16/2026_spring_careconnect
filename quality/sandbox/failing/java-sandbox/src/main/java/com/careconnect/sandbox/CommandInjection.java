// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/CommandInjection.java
package com.careconnect.sandbox;

public class CommandInjection {

  public static void run(String userInput) throws Exception {
    // Finding (SpotBugs): command injection style pattern
    Runtime.getRuntime().exec("sh -c " + userInput);

    // Finding (PMD): pointless string concatenation
    String s = "" + userInput;
    if (s != null) {
    }
  }
}
