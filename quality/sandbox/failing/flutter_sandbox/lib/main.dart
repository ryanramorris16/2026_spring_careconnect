// File: /Volumes/DevDrive/code/2026_spring_careconnect/quality/sandbox/failing/flutter_sandbox/lib/main.dart

import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // Intentionally “bad practice” but NOT secret-like (avoid SECRET/AKIA/PASSWORD/etc.)
  static const String apiToken = "DEMO_TOKEN_NOT_REAL_123456";

  @override
  Widget build(BuildContext context) {
    // Intentional issue: unused local (analyzer warning)
    final unused = apiToken;

    // Intentional issue: always true condition (lint/analyzer)
    if (true) {
      debugPrint("Condition always true");
    }

    return const MaterialApp(
      home: Scaffold(
        body: Center(child: Text('FAIL')),
      ),
    );
  }
}