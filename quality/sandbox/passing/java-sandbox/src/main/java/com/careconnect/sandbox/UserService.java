package com.careconnect.sandbox;

import java.util.Objects;

/**
 * Simple service used by CI sandbox scans.
 */
public final class UserService {

  /**
   * Returns a deterministic greeting.
   *
   * @param user user to greet
   * @return greeting string
   */
  public String greet(User user) {
    Objects.requireNonNull(user, "user");
    return "Hello, " + user.getName() + "!";
  }
}