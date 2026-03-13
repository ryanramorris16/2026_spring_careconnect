// Tests for NewPostScreen
// (lib/features/social/presentation/pages/new_post_screen.dart).
//
// NewPostScreen reads UserProvider in build (not initState).
// Null user: shows "User not logged in" fallback immediately.
// Non-null user: shows the post creation form.
// No API calls in initState.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/social/presentation/pages/new_post_screen.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap({bool loggedIn = true}) {
  final provider = loggedIn
      ? MockUserProvider(mockUser: MockUser(id: 1, role: 'PATIENT'))
      : _NullUserProvider();
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const NewPostScreen(),
    ),
  );
}

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

void main() {
  group('NewPostScreen – logged-in user', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(NewPostScreen), findsOneWidget);
    });

    testWidgets('shows "Create New Post" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Create New Post'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "Post" button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Post'), findsOneWidget);
    });

    testWidgets('does NOT show "User not logged in" for logged-in user',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('User not logged in'), findsNothing);
    });
  });

  group('NewPostScreen – null user', () {
    testWidgets('shows "User not logged in" when user is null', (tester) async {
      await tester.pumpWidget(_wrap(loggedIn: false));
      expect(find.text('User not logged in'), findsOneWidget);
    });

    testWidgets('shows "Create New Post" AppBar even when null user',
        (tester) async {
      await tester.pumpWidget(_wrap(loggedIn: false));
      expect(find.text('Create New Post'), findsOneWidget);
    });
  });
}
