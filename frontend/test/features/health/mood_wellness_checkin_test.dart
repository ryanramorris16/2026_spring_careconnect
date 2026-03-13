// Tests for MoodWellnessCheckIn
// (lib/features/health/presentation/pages/mood_wellness_checkin.dart).
//
// No initState override and no API calls at startup.
// Submission is triggered by user action only.
// Uses larger viewport to avoid Column overflow.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/presentation/pages/mood_wellness_checkin.dart';

Widget _wrap() => const MaterialApp(home: MoodWellnessCheckIn());

void _setLargeViewport(WidgetTester tester) {
  tester.view.physicalSize = const Size(800, 1600);
  tester.view.devicePixelRatio = 1.0;
}

void main() {
  group('MoodWellnessCheckIn – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      expect(find.byType(MoodWellnessCheckIn), findsOneWidget);
    });

    testWidgets('shows "Mood & Wellness Check-In" in AppBar', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      expect(find.text('Mood & Wellness Check-In'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator initially',
        (tester) async {
      _setLargeViewport(tester);
      addTearDown(tester.view.resetPhysicalSize);
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
