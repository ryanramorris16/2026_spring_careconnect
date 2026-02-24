import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  static const String apiToken = 'DEMO_TOKEN_NOT_REAL_123456';

  @override
  Widget build(BuildContext context) {
    // Intentional violation: unused local variable
    final unused = apiToken;

    // Intentional violation: condition always evaluates to true
    if (true) {
      debugPrint('Condition always true');
    }

    return const MaterialApp(
      home: Scaffold(
        body: Center(child: Text('FAIL')),
      ),
    );
  }
}