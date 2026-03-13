// Tests for RBACTestScreen
// (lib/screens/rbac_test_screen.dart).
//
// Uses Provider.of<UserProvider> in build.
// With userSession=null (default MockUserProvider), shows role-selection UI.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/screens/rbac_test_screen.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'PATIENT'),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const RBACTestScreen(),
    ),
  );
}

void main() {
  group('RBACTestScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(RBACTestScreen), findsOneWidget);
    });

    testWidgets('shows "RBAC Test" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('RBAC Test'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "Select a role to test RBAC:" when user has no session',
        (tester) async {
      // MockUserProvider.userSession is null by default (raw var field, not overridden)
      await tester.pumpWidget(_wrap());
      expect(find.text('Select a role to test RBAC:'), findsOneWidget);
    });

    testWidgets('shows "Login as Admin" button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Login as Admin'), findsOneWidget);
    });
  });
}
