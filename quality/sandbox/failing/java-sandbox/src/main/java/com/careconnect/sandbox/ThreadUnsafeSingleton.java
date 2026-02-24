// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/ThreadUnsafeSingleton.java
package com.careconnect.sandbox;

public class ThreadUnsafeSingleton {

  // Finding (SpotBugs): lazy init without synchronization
  private static ThreadUnsafeSingleton instance;

  private ThreadUnsafeSingleton() {}

  public static ThreadUnsafeSingleton getInstance() {
    if (instance == null) {
      instance = new ThreadUnsafeSingleton();
    }
    return instance;
  }
}
