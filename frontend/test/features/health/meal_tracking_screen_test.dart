// Tests for MealTrackingScreen
// (lib/features/health/presentation/pages/meal_tracking_screen.dart).
//
// initState calls _loadMealQuestions() which is synchronous (no HTTP).
// No Provider needed. Pure form widget.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/presentation/pages/meal_tracking_screen.dart';

Widget _wrap() => const MaterialApp(home: MealTrackingScreen());

void main() {
  group('MealTrackingScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(MealTrackingScreen), findsOneWidget);
    });

    testWidgets('shows "Meal & Nutrition Tracking" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Meal & Nutrition Tracking'), findsOneWidget);
    });

    testWidgets('shows instruction text', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(
        find.text('Please answer the following questions:'),
        findsOneWidget,
      );
    });

    testWidgets('shows multiple TextFields for meal responses', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(TextField), findsWidgets);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
