// Tests for TelehealthBridgeScreen
// (lib/features/calls/presentation/pages/telehealth_bridge_screen.dart).
//
// StatefulWidget with hardcoded appointment data — no HTTP or Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/calls/presentation/pages/telehealth_bridge_screen.dart';

Widget _wrap() => const MaterialApp(home: TelehealthBridgeScreen());

void main() {
  group('TelehealthBridgeScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(TelehealthBridgeScreen), findsOneWidget);
    });

    testWidgets('shows "Telehealth Bridge" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Telehealth Bridge'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows doctor names in appointment list', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Dr. Smith'), findsWidgets);
    });

    testWidgets('shows AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(AppBar), findsOneWidget);
    });

    testWidgets('shows today\'s appointments as Card widgets', (tester) async {
      await tester.pumpWidget(_wrap());
      // Two appointments are for today (Dr. Smith and Dr. Johnson)
      expect(find.byType(Card), findsNWidgets(2));
    });

    testWidgets('shows ListTile for each today appointment', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ListTile), findsNWidgets(2));
    });

    testWidgets('shows "Join" buttons for today\'s appointments', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Join'), findsNWidgets(2));
    });

    testWidgets('shows ElevatedButton for each appointment', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ElevatedButton), findsNWidgets(2));
    });

    testWidgets('shows Dr. Johnson appointment', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Dr. Johnson'), findsOneWidget);
    });

    testWidgets('shows appointment time text', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('10:00 AM'), findsOneWidget);
      expect(find.textContaining('2:00 PM'), findsOneWidget);
    });

    testWidgets('shows horizontal date calendar', (tester) async {
      await tester.pumpWidget(_wrap());
      // Calendar is a horizontal ListView with 14 days
      expect(find.byType(ListView), findsOneWidget);
    });

    testWidgets('shows GestureDetector for each calendar date', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(GestureDetector), findsWidgets);
    });

    testWidgets('does NOT show Dr. Williams on today (tomorrow\'s appointment)',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Dr. Williams'), findsNothing);
    });
  });

  group('TelehealthBridgeScreen – date selection', () {
    testWidgets('tapping tomorrow date shows Dr. Williams', (tester) async {
      await tester.pumpWidget(_wrap());
      // Find all GestureDetectors in the calendar; the second one is tomorrow.
      // The calendar renders 14 date containers as GestureDetectors.
      // We tap the second date item (index 1 = tomorrow).
      final containers = find.byType(GestureDetector);
      // Tap the second calendar date (tomorrow)
      await tester.tap(containers.at(1));
      await tester.pump();
      expect(find.textContaining('Dr. Williams'), findsOneWidget);
    });

    testWidgets('tapping tomorrow hides today\'s appointments', (tester) async {
      await tester.pumpWidget(_wrap());
      final containers = find.byType(GestureDetector);
      await tester.tap(containers.at(1));
      await tester.pump();
      expect(find.textContaining('Dr. Smith'), findsNothing);
      expect(find.textContaining('Dr. Johnson'), findsNothing);
    });

    testWidgets('tapping a date with no appointments shows empty message',
        (tester) async {
      await tester.pumpWidget(_wrap());
      // Tap the 4th date (index 3 = 3 days from now — no appointments)
      final containers = find.byType(GestureDetector);
      await tester.tap(containers.at(3));
      await tester.pump();
      expect(find.text('No appointments for this day.'), findsOneWidget);
    });

    testWidgets('empty day shows no Card widgets', (tester) async {
      await tester.pumpWidget(_wrap());
      final containers = find.byType(GestureDetector);
      await tester.tap(containers.at(3));
      await tester.pump();
      expect(find.byType(Card), findsNothing);
    });

    testWidgets('tomorrow shows exactly one appointment card', (tester) async {
      await tester.pumpWidget(_wrap());
      final containers = find.byType(GestureDetector);
      await tester.tap(containers.at(1));
      await tester.pump();
      expect(find.byType(Card), findsOneWidget);
      expect(find.text('Join'), findsOneWidget);
    });

    testWidgets('tapping back to today restores both appointments',
        (tester) async {
      await tester.pumpWidget(_wrap());
      final containers = find.byType(GestureDetector);
      // Go to tomorrow
      await tester.tap(containers.at(1));
      await tester.pump();
      expect(find.byType(Card), findsOneWidget);
      // Go back to today
      await tester.tap(containers.at(0));
      await tester.pump();
      expect(find.byType(Card), findsNWidgets(2));
    });
  });
}
