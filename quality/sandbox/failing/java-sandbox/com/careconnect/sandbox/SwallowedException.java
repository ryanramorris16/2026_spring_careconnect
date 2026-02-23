// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/SwallowedException.java
package com.careconnect.sandbox;

public class SwallowedException {

  public static int parse(String s) {
    // Finding (PMD): avoid catching generic Exception + swallowing
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }

  public static void badStyle( ) {     // Finding (Checkstyle): spacing / formatting bait
    int x=1;                           // Finding (Checkstyle/PMD): missing spaces
    if(x==1){System.out.println(x);}   // Finding (Checkstyle): braces/spacing
  }
}
