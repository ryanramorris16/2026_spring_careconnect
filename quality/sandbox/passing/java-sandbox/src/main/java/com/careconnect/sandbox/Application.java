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
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(new UserService().greet(new AppUser("u-001", "Alex")));
    }
  }
}