// Tests for SchedulePage
// (lib/features/evv/schedule/pages/schedule_page.dart).
//
// initState calls multiple load methods which use Provider.of<UserProvider>.
// _isLoading starts false but is set to true synchronously.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/schedule/pages/schedule_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'CAREGIVER', caregiverId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const SchedulePage(),
    ),
  );
}

void main() {
  group('SchedulePage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(SchedulePage), findsOneWidget);
    });

    testWidgets('shows "EVV Visit Schedules" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('EVV Visit Schedules'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // _loadScheduledVisits() sets _isLoading=true synchronously.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
