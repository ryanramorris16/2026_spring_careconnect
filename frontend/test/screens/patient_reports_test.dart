// Tests for PatientReportsScreen
// (lib/screens/patient_reports.dart).
//
// Pure StatelessWidget with no API calls or Provider dependencies.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/screens/patient_reports.dart';

Widget _wrap() => const MaterialApp(home: PatientReportsScreen());

void main() {
  group('PatientReportsScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(PatientReportsScreen), findsOneWidget);
    });

    testWidgets('shows "My Reports" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('My Reports'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
