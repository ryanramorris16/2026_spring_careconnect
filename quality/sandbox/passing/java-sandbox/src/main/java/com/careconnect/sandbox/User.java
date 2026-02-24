package com.careconnect.sandbox;

import java.util.Objects;

/**
 * Simple POJO used by CI sandbox scans.
 */
public final class User {

  private final String id;
  private final String name;

  /**
   * Creates a user.
   *
   * @param id unique identifier
   * @param name display name
   */
  public User(String id, String name) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Objects.requireNonNull(name, "name");
  }

  /**
   * Returns the unique identifier.
   *
   * @return unique identifier
   */
  public String getId() {

    return id;
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