// Tests for LeaderboardScreen
// (lib/features/gamification/presentation/pages/leaderboard_screen.dart).
//
// LeaderboardScreen calls fetchLeaderboard() in initState but renders a
// CircularProgressIndicator while isLoading=true (the initial state).
// The HTTP request fails silently in tests (catch prints error, no setState),
// so the widget remains in the loading state — safe to test that initial render.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/gamification/presentation/pages/leaderboard_screen.dart';

Widget _wrap() =>
    const MaterialApp(home: LeaderboardScreen());

void main() {
  group('LeaderboardScreen – initial (loading) state', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      // Do NOT call pumpAndSettle — that would wait for the HTTP call forever.
      expect(find.byType(LeaderboardScreen), findsOneWidget);
    });

    testWidgets('shows "Leaderboard" in the AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Leaderboard'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // On first render isLoading=true, so a progress indicator is shown.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('does NOT show leaderboard list while loading', (tester) async {
      // No ListView should be visible while isLoading=true.
      await tester.pumpWidget(_wrap());
      expect(find.byType(ListView), findsNothing);
    });
  });
}
