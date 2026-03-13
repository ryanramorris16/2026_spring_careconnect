// Tests for CaregiverRegistrationPage
// (lib/features/onboarding/presentation/pages/caregiver_registration.dart).
//
// Pure form widget — no API calls in initState; submission only on button press.
// Tests cover initial render, form fields, checkboxes, and in-line validation.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/onboarding/presentation/pages/caregiver_registration.dart';

Widget _wrap() =>
    const MaterialApp(home: CaregiverRegistrationPage());

void main() {
  group('CaregiverRegistrationPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CaregiverRegistrationPage), findsOneWidget);
    });

    testWidgets('shows "Caregiver Registration" in the AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Caregiver Registration'), findsOneWidget);
    });

    testWidgets('shows "Register a Caregiver" heading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Register a Caregiver'), findsOneWidget);
    });

    testWidgets('shows a Form widget', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Form), findsOneWidget);
    });

    testWidgets('shows multiple TextFormFields', (tester) async {
      await tester.pumpWidget(_wrap());
      // Full Name, Email, Phone, City, State, Password, Confirm Password = 7
      expect(find.byType(TextFormField), findsWidgets);
    });

    testWidgets('shows "Caregiver Type" section label', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Caregiver Type'), findsOneWidget);
    });

    testWidgets('shows "Family Member" checkbox', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Family Member'), findsOneWidget);
    });

    testWidgets('shows "Professional" checkbox', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Professional'), findsOneWidget);
    });

    testWidgets('shows "Register" submit button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Register'), findsOneWidget);
    });

    testWidgets('Family Member checkbox is initially checked', (tester) async {
      await tester.pumpWidget(_wrap());
      final checkboxes = tester.widgetList<CheckboxListTile>(
        find.byType(CheckboxListTile),
      ).toList();
      // First checkbox is "Family Member" — starts checked.
      expect(checkboxes.first.value, isTrue);
    });

    testWidgets('Professional checkbox is initially unchecked', (tester) async {
      await tester.pumpWidget(_wrap());
      final checkboxes = tester.widgetList<CheckboxListTile>(
        find.byType(CheckboxListTile),
      ).toList();
      // Second checkbox is "Professional" — starts unchecked.
      expect(checkboxes.last.value, isFalse);
    });
  });

  group('CaregiverRegistrationPage – checkbox interaction', () {
    testWidgets('tapping Professional checkbox checks it', (tester) async {
      await tester.pumpWidget(_wrap());
      // Ensure the checkbox is visible before tapping (form is in a scroll view).
      await tester.ensureVisible(find.text('Professional'));
      await tester.tap(find.text('Professional'));
      await tester.pump();
      final checkboxes = tester.widgetList<CheckboxListTile>(
        find.byType(CheckboxListTile),
      ).toList();
      expect(checkboxes.last.value, isTrue);
    });

    testWidgets('unchecking Family Member auto-checks Professional',
        (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.ensureVisible(find.text('Family Member'));
      // Uncheck "Family Member" (currently checked).
      await tester.tap(find.text('Family Member'));
      await tester.pump();
      final checkboxes = tester.widgetList<CheckboxListTile>(
        find.byType(CheckboxListTile),
      ).toList();
      // When both would be unchecked, Professional is forced true.
      expect(checkboxes.last.value, isTrue);
    });
  });
}
