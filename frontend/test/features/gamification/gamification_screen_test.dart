// Tests for GamificationScreen
// (lib/features/gamification/presentation/pages/gamification_screen.dart).
//
// initState creates ConfettiController and calls initializePrefsAndLoad()
// (async — SharedPreferences + GamificationService HTTP).
// isLoading = true initially.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:care_connect_app/features/gamification/presentation/pages/gamification_screen.dart';

Widget _wrap() => const MaterialApp(home: GamificationScreen());

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('GamificationScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(GamificationScreen), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // isLoading = true on first frame before async data resolves.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
