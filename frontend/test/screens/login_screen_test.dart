// Tests for LoginScreen
// (lib/screens/login_screen.dart).
//
// LoginScreen is a pure form widget — no API calls in initState.
// AuthService is only invoked on button press.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/screens/login_screen.dart';

Widget _wrap() => const MaterialApp(home: LoginScreen());

void main() {
  group('LoginScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(LoginScreen), findsOneWidget);
    });

    testWidgets('shows "CareConnect" title', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('CareConnect'), findsOneWidget);
    });

    testWidgets('shows "Login to continue" subtitle', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Login to continue'), findsOneWidget);
    });

    testWidgets('shows Icons.health_and_safety logo', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byIcon(Icons.health_and_safety), findsOneWidget);
    });

    testWidgets('shows Username TextFormField', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.widgetWithText(TextFormField, 'Username'), findsOneWidget);
    });

    testWidgets('shows Password TextFormField', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.widgetWithText(TextFormField, 'Password'), findsOneWidget);
    });

    testWidgets('shows "Login" button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Login'), findsOneWidget);
    });

    testWidgets('shows Icons.person in username field', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byIcon(Icons.person), findsOneWidget);
    });

    testWidgets('shows Icons.lock in password field', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byIcon(Icons.lock), findsOneWidget);
    });

    testWidgets('shows Demo Accounts expansion tile', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Demo Accounts'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows Form widget', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Form), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator initially',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
