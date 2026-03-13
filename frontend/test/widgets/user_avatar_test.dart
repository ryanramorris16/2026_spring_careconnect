// Tests for UserAvatar widget
// (lib/widgets/user_avatar.dart).
//
// Pure StatelessWidget — reads imageUrl, resolves it, and renders a
// CircleAvatar.  When imageUrl is null or empty it falls back to an
// Icons.person icon; otherwise it sets a NetworkImage as backgroundImage.
//
// Tests that supply a real URL are omitted because flutter_test's network
// image loading triggers FlutterError.onError on failure, which fails the
// test.  Only the null/empty-URL (fallback icon) path is tested here.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/user_avatar.dart';

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

void main() {
  group('UserAvatar – null / empty imageUrl (fallback icon)', () {
    testWidgets('renders without crashing with null imageUrl', (tester) async {
      // Verifies the widget builds without error when no URL is supplied.
      await tester.pumpWidget(_wrap(const UserAvatar(imageUrl: null)));
      expect(find.byType(UserAvatar), findsOneWidget);
    });

    testWidgets('shows Icons.person when imageUrl is null', (tester) async {
      // The fallback icon must be rendered when no URL is provided.
      await tester.pumpWidget(_wrap(const UserAvatar(imageUrl: null)));
      expect(find.byIcon(Icons.person), findsOneWidget);
    });

    testWidgets('shows Icons.person when imageUrl is empty string',
        (tester) async {
      // An empty string is treated as "no URL", so the icon fallback applies.
      await tester.pumpWidget(_wrap(const UserAvatar(imageUrl: '')));
      expect(find.byIcon(Icons.person), findsOneWidget);
    });

    testWidgets('renders a CircleAvatar', (tester) async {
      // The underlying widget is always a CircleAvatar.
      await tester.pumpWidget(_wrap(const UserAvatar(imageUrl: null)));
      expect(find.byType(CircleAvatar), findsOneWidget);
    });

    testWidgets('uses custom radius', (tester) async {
      // Verifies the radius parameter is forwarded to CircleAvatar.
      await tester.pumpWidget(
          _wrap(const UserAvatar(imageUrl: null, radius: 40)));
      final avatar =
          tester.widget<CircleAvatar>(find.byType(CircleAvatar));
      expect(avatar.radius, 40.0);
    });

    testWidgets('default radius is 20', (tester) async {
      // Without an explicit radius, the widget uses 20.
      await tester.pumpWidget(_wrap(const UserAvatar(imageUrl: null)));
      final avatar =
          tester.widget<CircleAvatar>(find.byType(CircleAvatar));
      expect(avatar.radius, 20.0);
    });
  });

}
