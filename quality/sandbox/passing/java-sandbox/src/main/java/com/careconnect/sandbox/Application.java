package com.careconnect.sandbox;

/**
 * Minimal Java entrypoint used by CI sandbox scans.
 */
public final class Application {

  private Application() {
    // Utility class; prevent instantiation.
  }

  /**
   * Program entrypoint.
   *
   * @param args command line args
   */
  public static void main(String[] args) {
    UserService service = new UserService();
    User user = new User("u-001", "Alex");
    System.out.println(service.greet(user));
  }
}