// Tests for UspsTestScreen
// (lib/features/usps/presentation/usps_test_screen.dart).
//
// _checkGoogleConnection() is called in initState (via addPostFrameCallback)
// and has a full try/catch — Dio failure leaves isGoogleConnected = false.
// loading starts false, so the full body is rendered on first pump().
// Tests wrap with MockUserProvider to satisfy Provider.of<UserProvider>.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/usps/presentation/usps_test_screen.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

// Use null user so _checkGoogleConnection() returns early (no Dio call, no
// pending timers that would fail the test teardown).
Widget _wrap() {
  final provider = _NullUserProvider();
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const UspsTestScreen(),
    ),
  );
}

/// Minimal provider that always returns null user.
class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

void _setLargeViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 1600);
  tester.view.devicePixelRatio = 1.0;
}

void main() {
  group('UspsTestScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(UspsTestScreen), findsOneWidget);
    });

    testWidgets('shows "USPS Mail Digest" in the AppBar', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('USPS Mail Digest'), findsOneWidget);
    });

    testWidgets('shows "Gmail Integration" card heading', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Gmail Integration'), findsOneWidget);
    });

    testWidgets('shows not-connected message when Google not linked',
        (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(
        find.textContaining('Connect your Google account'),
        findsOneWidget,
      );
    });

    testWidgets('shows "Connect Google Account" button when not connected',
        (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Connect Google Account'), findsOneWidget);
    });

    testWidgets('shows Icons.mail icon in Gmail Integration card',
        (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byIcon(Icons.mail), findsOneWidget);
    });

    testWidgets('shows Icons.link icon on Connect button', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byIcon(Icons.link), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator on initial render',
        (tester) async {
      // loading starts false — no spinner shown
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
