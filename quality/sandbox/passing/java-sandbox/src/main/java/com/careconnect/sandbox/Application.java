package com.careconnect.sandbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minimal Java entrypoint used by CI sandbox scans.
 */
public final class Application {

  private static final Logger LOGGER = LogManager.getLogger(Application.class);

  private Application() {
    // Utility class; prevent instantiation.
  }

  /**
   * Program entrypoint.
   *
   * @param args command line args
   */
  public static void main(final String[] args) {
    final UserService service = new UserService();
    final AppUser user = new AppUser("u-001", "Alex");
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(service.greet(user));
    }
  }
}