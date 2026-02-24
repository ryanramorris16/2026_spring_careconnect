// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/flutter_sandbox/lib/utils/helper.dart

int calculate(int a, int b) {
  // Intentional issue: pointless condition (lint/analyzer)
  if (a == a) {
    // no-op
  }

  // Intentional issue: unused local
  final temp = a + b;

  return a + b;
}