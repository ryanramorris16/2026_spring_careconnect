// Tests for PatientRegistrationPage
// (lib/features/onboarding/presentation/pages/patient_registration.dart).
//
// Pure multi-step form widget — no API calls in initState.
// Tests cover initial render and Stepper structure.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/onboarding/presentation/pages/patient_registration.dart';

Widget _wrap({int? caregiverId}) =>
    MaterialApp(home: PatientRegistrationPage(caregiverId: caregiverId));

void main() {
  group('PatientRegistrationPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(PatientRegistrationPage), findsOneWidget);
    });

    testWidgets('shows "Register New Patient" in the AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Register New Patient'), findsOneWidget);
    });

    testWidgets('shows Stepper widget', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Stepper), findsOneWidget);
    });

    testWidgets('shows "Personal Information" step title', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Personal Information'), findsOneWidget);
    });

    testWidgets('shows Form widgets (one per step)', (tester) async {
      await tester.pumpWidget(_wrap());
      // Each step wraps its fields in a Form.
      expect(find.byType(Form), findsWidgets);
    });

    testWidgets('shows "Next" button on first step', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Next'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows multiple TextFormFields', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(TextFormField), findsWidgets);
    });

    testWidgets('does NOT show CircularProgressIndicator initially',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
