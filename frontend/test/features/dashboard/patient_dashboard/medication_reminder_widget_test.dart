// Tests for MedicationRemindersWidget
// (lib/features/dashboard/patient_dashboard/widgets/medication_reminder_widget.dart).
//
// Pure StatelessWidget — no Provider, no HTTP.
// With reminder=null shows "None to show" message.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/dashboard/patient_dashboard/widgets/medication_reminder_widget.dart';

Widget _wrapNull() => const MaterialApp(
      home: Scaffold(body: MedicationRemindersWidget(reminder: null)),
    );

Widget _wrapWithReminder() => MaterialApp(
      home: Scaffold(
        body: MedicationRemindersWidget(
          reminder: MedicationReminder(
            medicationName: 'Aspirin',
            scheduledTime: DateTime(2025, 1, 1, 9),
            status: 'pending',
          ),
        ),
      ),
    );

void main() {
  group('MedicationRemindersWidget – null reminder', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrapNull());
      expect(find.byType(MedicationRemindersWidget), findsOneWidget);
    });

    testWidgets('shows "None to show" when reminder is null', (tester) async {
      await tester.pumpWidget(_wrapNull());
      expect(find.textContaining('None'), findsOneWidget);
    });
  });

  group('MedicationRemindersWidget – with reminder', () {
    testWidgets('shows medication name', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('Aspirin'), findsOneWidget);
    });
  });
}
