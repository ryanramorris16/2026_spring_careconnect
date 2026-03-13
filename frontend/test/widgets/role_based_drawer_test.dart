// Tests for RoleBasedDrawer
// (lib/widgets/role_based_drawer.dart).
//
// RoleBasedDrawer is a StatelessWidget using Consumer<UserProvider>.
// When userSession is null it shows "Not logged in" fallback.
// No API calls in initState.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/widgets/role_based_drawer.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../mock_user_provider.dart';

// Wraps the drawer inside a Scaffold so it can be opened.
Widget _wrap({UserProvider? provider}) {
  final p = provider ?? MockUserProvider(mockUser: MockUser());
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: p,
      child: Scaffold(
        drawer: const RoleBasedDrawer(),
        body: Builder(
          builder: (ctx) => TextButton(
            onPressed: () => Scaffold.of(ctx).openDrawer(),
            child: const Text('Open'),
          ),
        ),
      ),
    ),
  );
}

void main() {
  group('RoleBasedDrawer – null userSession', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      // Open the drawer
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      expect(find.byType(Drawer), findsOneWidget);
    });

    testWidgets('shows "Not logged in" when userSession is null', (tester) async {
      // MockUserProvider.userSession is null by default (inherited var field)
      await tester.pumpWidget(_wrap());
      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();
      expect(find.text('Not logged in'), findsOneWidget);
    });
  });
}
