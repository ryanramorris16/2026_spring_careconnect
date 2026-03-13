// Tests for CommonDrawer
// (lib/widgets/common_drawer.dart).
//
// Uses Provider.of<UserProvider> in build and initState.
// _loadProfilePicture() checks userId (null if no user); no HTTP if null.
// Uses MockUserProvider with null user to avoid HTTP calls.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/widgets/common_drawer.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../mock_user_provider.dart';

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

Widget _wrap() {
  final provider = _NullUserProvider();
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: Scaffold(
        drawer: const CommonDrawer(currentRoute: '/dashboard'),
        body: const SizedBox(),
      ),
    ),
  );
}

void main() {
  group('CommonDrawer – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('opens drawer and shows "Please log in"', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      // Open the drawer
      final ScaffoldState scaffold = tester.state(find.byType(Scaffold));
      scaffold.openDrawer();
      await tester.pump();
      expect(find.text('Please log in'), findsOneWidget);
    });
  });
}
