package com.careconnect.sandbox;

import java.util.Objects;

/**
 * Simple service used by CI sandbox scans.
 */
public final class UserService {

  /**
   * Default constructor.
   */
  @SuppressWarnings("PMD.UnnecessaryConstructor")
  public UserService() {
    // Explicit constructor required by PMD AtLeastOneConstructor.
  }

  /**
   * Returns a deterministic greeting.
   *
   * @param user user to greet
   * @return greeting string
   */
  public String greet(final AppUser user) {
    Objects.requireNonNull(user, "user");
    return "Hello, " + user.getName() + "!";
  }
}