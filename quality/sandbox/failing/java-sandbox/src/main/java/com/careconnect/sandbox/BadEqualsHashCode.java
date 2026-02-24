// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/BadEqualsHashCode.java
package com.careconnect.sandbox;

public class BadEqualsHashCode {

  private final int id;
  private final String name;

  public BadEqualsHashCode(int id, String name) {
    this.id = id;
    this.name = name;
  }

  // Finding (SpotBugs): equals without hashCode
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BadEqualsHashCode)) return false;
    BadEqualsHashCode other = (BadEqualsHashCode) o;
    return this.id == other.id && (this.name == null ? other.name == null : this.name.equals(other.name));
  }
}
