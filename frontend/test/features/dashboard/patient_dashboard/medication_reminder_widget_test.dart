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

    testWidgets('does NOT show medication icon when null', (tester) async {
      await tester.pumpWidget(_wrapNull());
      expect(find.byIcon(Icons.medication), findsNothing);
    });

    testWidgets('does NOT show Mark Taken button when null', (tester) async {
      await tester.pumpWidget(_wrapNull());
      expect(find.text('Mark Taken'), findsNothing);
    });
  });

  group('MedicationRemindersWidget – with reminder', () {
    testWidgets('shows medication name', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('Aspirin'), findsOneWidget);
    });

    testWidgets('shows Medication Reminders heading', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('Medication Reminders'), findsOneWidget);
    });

    testWidgets('shows medication icon', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.byIcon(Icons.medication), findsOneWidget);
    });

    testWidgets('shows Mark Taken button', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('Mark Taken'), findsOneWidget);
    });

    testWidgets('shows Mark Missed button', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('Mark Missed'), findsOneWidget);
    });

    testWidgets('shows status text', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.text('pending'), findsOneWidget);
    });

    testWidgets('shows two OutlinedButton widgets', (tester) async {
      await tester.pumpWidget(_wrapWithReminder());
      expect(find.byType(OutlinedButton), findsNWidgets(2));
    });
  });
}
