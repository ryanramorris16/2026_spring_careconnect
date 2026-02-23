// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/java-sandbox/com/careconnect/sandbox/SqlInjection.java
package com.careconnect.sandbox;

import java.sql.Connection;
import java.sql.Statement;

public class SqlInjection {

  public static void run(Connection conn, String userInput) throws Exception {
    // Finding (SpotBugs): SQL built via concatenation
    String query = "SELECT * FROM users WHERE name='" + userInput + "'";
    Statement st = conn.createStatement();
    st.execute(query);

    // Finding (PMD): empty catch
    try {
      Integer.parseInt("not-a-number");
    } catch (Exception e) {
    }
  }
}
