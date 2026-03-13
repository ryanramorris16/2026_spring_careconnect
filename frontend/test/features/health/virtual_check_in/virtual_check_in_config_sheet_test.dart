// Tests for VirtualCheckInConfigSheet
// (lib/features/health/virtual_check_in/presentation/widgets/virtual_check_in_config_sheet.dart).
//
// initState calls _api.getQuestions() and _qApi.listQuestions() (HTTP).
// _loading=true initially — loading spinner shown before HTTP resolves.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/virtual_check_in/presentation/widgets/virtual_check_in_config_sheet.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: VirtualCheckInConfigSheet(
          checkInId: 1,
          initial: const [],
        ),
      ),
    );

void main() {
  group('VirtualCheckInConfigSheet – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(VirtualCheckInConfigSheet), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // _loading=true on first frame while HTTP resolves.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
