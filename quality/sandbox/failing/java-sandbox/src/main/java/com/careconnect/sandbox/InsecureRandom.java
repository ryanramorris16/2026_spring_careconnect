// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/InsecureRandom.java
package com.careconnect.sandbox;

import java.util.Random;

public class InsecureRandom {

  // Finding (SpotBugs): predictable RNG for security usage
  public static String token() {
    Random r = new Random(12345); // fixed seed
    return Long.toHexString(r.nextLong()) + Long.toHexString(r.nextLong());
  }

  public static void main(String[] args) {
    // Finding (Secrets): looks like a private key header
    String pemHeader = "-----BEGIN PRIVATE KEY-----";
    System.out.println(pemHeader + token());
  }
}
