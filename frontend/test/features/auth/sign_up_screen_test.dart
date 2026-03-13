// Tests for RegistrationPage
// (lib/features/auth/presentation/pages/sign_up_screen.dart).
//
// Multi-step registration form — no API calls in initState.
// Navigation uses context.go only on button press, so GoRouter not needed.
// Tests cover initial step 0 (Account Role) render and UI element presence.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/auth/presentation/pages/sign_up_screen.dart';

Widget _wrap({String? initialRole}) => MaterialApp(
      home: RegistrationPage(initialRole: initialRole),
    );

void main() {
  group('RegistrationPage – initial render (step 0)', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(RegistrationPage), findsOneWidget);
    });

    testWidgets('shows "Create Your CareConnect Account" title', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Create Your CareConnect Account'), findsOneWidget);
    });

    testWidgets('shows "Join our secure healthcare platform" subtitle',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Join our secure healthcare platform'), findsOneWidget);
    });

    testWidgets('shows progress heading "Step 1 of"', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Step 1 of'), findsOneWidget);
    });

    testWidgets('shows "Account Role" step label at step 0', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Account Role'), findsOneWidget);
    });

    testWidgets('shows "Back to Login" text button at step 0', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Back to Login'), findsOneWidget);
    });

    testWidgets('shows "Next" button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Next'), findsOneWidget);
    });

    testWidgets('shows a Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });

  group('RegistrationPage – with preselected role', () {
    testWidgets('renders without crashing with Patient role', (tester) async {
      await tester.pumpWidget(_wrap(initialRole: 'Patient'));
      expect(find.byType(RegistrationPage), findsOneWidget);
    });

    testWidgets('renders without crashing with Caregiver role', (tester) async {
      await tester.pumpWidget(_wrap(initialRole: 'Caregiver'));
      expect(find.byType(RegistrationPage), findsOneWidget);
    });
  });
}
