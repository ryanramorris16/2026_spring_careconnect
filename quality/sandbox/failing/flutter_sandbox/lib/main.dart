import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // Finding #1 — hardcoded secret pattern (Semgrep / secret scanners)
  static const String apiToken = "SECRET_DEMO_FLUTTER_TOKEN_123456";

  @override
  Widget build(BuildContext context) {
    // Finding #2 — unused variable (Flutter analyzer warning)
    final unused = apiToken;

    // Finding #3 — always true condition (lint-style issue)
    if (true) {
      debugPrint("Condition always true");
    }

    return const MaterialApp(
      home: Scaffold(
        body: Center(
          child: Text('FAIL'),
        ),
      ),
    );
  }
}