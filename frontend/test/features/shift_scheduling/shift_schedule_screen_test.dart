// Tests for CaregiverShiftSchedulingScreen
// (lib/features/shift_scheduling/presentation/shift_schedule_screen.dart).
//
// Pure StatefulWidget — no API calls, no Provider.
// Contains a SwitchListTile for recurring toggle, Start/End time pickers,
// ChoiceChips for days of the week, and a Save ElevatedButton.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/shift_scheduling/presentation/shift_schedule_screen.dart';

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  group('CaregiverShiftSchedulingScreen', () {
    testWidgets('renders without crashing', (tester) async {
      // Verifies the screen builds without error.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.byType(CaregiverShiftSchedulingScreen), findsOneWidget);
    });

    testWidgets('shows "Caregiver Shift Scheduling" in AppBar', (tester) async {
      // The AppBar title must identify the screen.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('Caregiver Shift Scheduling'), findsOneWidget);
    });

    testWidgets('shows "Recurring Shift" SwitchListTile', (tester) async {
      // The recurring shift toggle must be visible.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('Recurring Shift'), findsOneWidget);
    });

    testWidgets('recurring shift toggle is OFF by default', (tester) async {
      // The switch starts unchecked (isRecurring = false).
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      final switchTile = tester.widget<SwitchListTile>(
        find.byType(SwitchListTile),
      );
      expect(switchTile.value, isFalse);
    });

    testWidgets('toggling switch turns recurring ON', (tester) async {
      // Tapping the SwitchListTile calls setState and flips the value.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      await tester.tap(find.byType(SwitchListTile));
      await tester.pump();
      final switchTile = tester.widget<SwitchListTile>(
        find.byType(SwitchListTile),
      );
      expect(switchTile.value, isTrue);
    });

    testWidgets('shows "Start Time" label', (tester) async {
      // The Start Time section header must be visible.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('Start Time'), findsOneWidget);
    });

    testWidgets('shows "End Time" label', (tester) async {
      // The End Time section header must be visible.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('End Time'), findsOneWidget);
    });

    testWidgets('shows "No time selected" for start time initially',
        (tester) async {
      // Before any time is picked, the ListTile shows a placeholder.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('No time selected'), findsWidgets);
    });

    testWidgets('shows "Days" section label', (tester) async {
      // The days-of-the-week section header must be present.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('Days'), findsOneWidget);
    });

    testWidgets('shows 7 ChoiceChips for days of the week', (tester) async {
      // One ChoiceChip per day: S M T W T F S.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.byType(ChoiceChip), findsNWidgets(7));
    });

    testWidgets('no day chip is selected by default', (tester) async {
      // selectedDayIndexes starts empty, so all chips are unselected.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      final chips = tester.widgetList<ChoiceChip>(find.byType(ChoiceChip));
      expect(chips.every((c) => c.selected == false), isTrue);
    });

    testWidgets('tapping a day chip selects it', (tester) async {
      // Selecting the first chip (Sunday) marks it as selected.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      await tester.tap(find.byType(ChoiceChip).first);
      await tester.pump();
      final chips = tester.widgetList<ChoiceChip>(find.byType(ChoiceChip));
      expect(chips.first.selected, isTrue);
    });

    testWidgets('shows ElevatedButton "Save"', (tester) async {
      // The Save button must be visible at the bottom.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.text('Save'), findsOneWidget);
      expect(find.byType(ElevatedButton), findsOneWidget);
    });

    testWidgets('shows at least one access_time icon', (tester) async {
      // Each time picker tile has an access_time leading icon.
      await tester.pumpWidget(
          _wrap(const CaregiverShiftSchedulingScreen()));
      expect(find.byIcon(Icons.access_time), findsWidgets);
    });
  });
}
