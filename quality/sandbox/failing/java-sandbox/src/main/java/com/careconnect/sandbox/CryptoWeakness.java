// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/CryptoWeakness.java
package com.careconnect.sandbox;

import java.security.MessageDigest;

public class CryptoWeakness {

  // Finding (Secrets): token-like string
  private static final String TOKEN = "SECRET_DEMO_JAVA_TOKEN_12345";

  public static String md5Hex(String input) throws Exception {
    // Finding (SpotBugs/PMD): weak hashing algorithm usage
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(input.getBytes("UTF-8"));
    StringBuilder sb = new StringBuilder();
    for (byte b : digest) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
