// Tests for SettingsScreen
// (lib/features/profile/presentation/pages/settings_screen.dart).
//
// initState calls loadUserInfo() which uses SharedPreferences (async, no API).
// No API calls, no Provider needed on initial render.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/profile/presentation/pages/settings_screen.dart';

Widget _wrap() => const MaterialApp(home: SettingsScreen());

void main() {
  group('SettingsScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(SettingsScreen), findsOneWidget);
    });

    testWidgets('shows "Settings" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Settings'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "Change Password" option', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump(); // let SharedPreferences resolve
      expect(find.text('Change Password'), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
