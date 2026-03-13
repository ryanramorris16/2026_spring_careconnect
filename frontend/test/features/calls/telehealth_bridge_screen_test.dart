// Tests for TelehealthBridgeScreen
// (lib/features/calls/presentation/pages/telehealth_bridge_screen.dart).
//
// StatefulWidget with hardcoded appointment data — no HTTP or Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/calls/presentation/pages/telehealth_bridge_screen.dart';

Widget _wrap() => const MaterialApp(home: TelehealthBridgeScreen());

void main() {
  group('TelehealthBridgeScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(TelehealthBridgeScreen), findsOneWidget);
    });

    testWidgets('shows "Telehealth Bridge" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Telehealth Bridge'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows doctor names in appointment list', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Dr. Smith'), findsWidgets);
    });
  });
}
