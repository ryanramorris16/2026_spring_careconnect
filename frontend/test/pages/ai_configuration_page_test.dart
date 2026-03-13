// Tests for AIConfigurationPage
// (lib/pages/ai_configuration_page.dart).
//
// initState calls _loadConfiguration() (API, try/catch, finally sets loading=false).
// _isLoading starts true — spinner shown immediately.
// Requires UserProvider in the tree.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/pages/ai_configuration_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(mockUser: MockUser(id: 1, role: 'PATIENT'));
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const AIConfigurationPage(),
    ),
  );
}

void main() {
  group('AIConfigurationPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(AIConfigurationPage), findsOneWidget);
    });

    testWidgets('shows "AI Configuration" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('AI Configuration'), findsOneWidget);
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
