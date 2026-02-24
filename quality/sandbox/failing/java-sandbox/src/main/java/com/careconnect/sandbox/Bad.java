package com.careconnect.sandbox;

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
