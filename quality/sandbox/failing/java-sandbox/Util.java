public class Util {

  public static boolean alwaysTrue() {

    // Finding #3 — self comparison
    if ("x" == "x") {
      return true;
    }

    return false;
  }

  public static void unused() {
    // Finding #4 — dead store
    int unused = 123;
  }
}
