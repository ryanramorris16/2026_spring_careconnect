package com.careconnect.sandbox;

public class UserService {

  // Finding #1 — hardcoded secret
  private static final String DB_PASSWORD = "HardcodedPassword!";

  public String process(String input) {

    // Finding #2 — potential null dereference
    if (input.equals("admin")) {
      return "admin";
    }

    return input;
  }
}

