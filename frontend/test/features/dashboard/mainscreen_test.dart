// Tests for EmergencyScreen from lib/features/dashboard/presentation/mainscreen.dart.
// Uses addPostFrameCallback to show the dialog.
// Pure StatefulWidget — no HTTP, no Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/dashboard/presentation/mainscreen.dart';

Widget _wrap() => const MaterialApp(home: EmergencyScreen());

void main() {
  group('EmergencyScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(EmergencyScreen), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows Emergency SOS dialog after first frame', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump(); // flush addPostFrameCallback
      expect(find.text('Emergency SOS'), findsOneWidget);
    });

    testWidgets('dialog has Yes, Send SOS button', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Yes, Send SOS'), findsOneWidget);
    });
  });
}
