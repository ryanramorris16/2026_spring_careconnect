int calculate(int a, int b) {
  // Finding #3 — unnecessary comparison
  if (a == a) {
    // no-op
  }

  // Finding #4 — unused variable
  final temp = a + b;

  return a + b;
}
