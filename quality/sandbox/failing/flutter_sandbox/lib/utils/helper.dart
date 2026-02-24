int calculate(int a, int b) {
  // Intentional violation: condition always evaluates to true
  if (a == a) {
    // no-op
  }

  // Intentional violation: unused local variable
  final temp = a + b;

  return a + b;
}