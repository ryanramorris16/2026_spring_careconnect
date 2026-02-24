package com.careconnect.sandbox;

import java.util.Objects;

/**
 * Simple domain object used by CI sandbox scans.
 */
public final class AppUser {

  private final String userId;
  private final String name;

  /**
   * Creates an AppUser.
   *
   * @param userId unique identifier
   * @param name   display name
   */
  public AppUser(final String userId, final String name) {
    this.userId = Objects.requireNonNull(userId, "userId");
    this.name = Objects.requireNonNull(name, "name");
  }

  /**
   * Returns the unique identifier.
   *
   * @return unique identifier
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Returns the display name.
   *
   * @return display name
   */
  public String getName() {
    return name;
  }
}