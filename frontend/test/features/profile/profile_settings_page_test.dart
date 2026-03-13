// Tests for ProfileSettingsPage
// (lib/features/profile/presentation/pages/profile_settings_page.dart).
//
// initState calls _loadUserProfile() which uses AuthTokenManager (try/catch).
// _isLoading starts true — spinner shown immediately.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/profile/presentation/pages/profile_settings_page.dart';

Widget _wrap() => const MaterialApp(home: ProfileSettingsPage());

void main() {
  group('ProfileSettingsPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ProfileSettingsPage), findsOneWidget);
    });

    testWidgets('shows "Profile Settings" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Profile Settings'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // _isLoading starts true; API call is pending.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
