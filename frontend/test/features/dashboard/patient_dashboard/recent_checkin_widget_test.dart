// Tests for RecentCheckInsWidget from
// lib/features/dashboard/patient_dashboard/widgets/recent_checkin_widget.dart.
// Pure StatelessWidget with checkIns list param.
// Provider.of<UserProvider> only used in button onPressed — not in build().

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/dashboard/patient_dashboard/widgets/recent_checkin_widget.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../../mock_user_provider.dart';

Widget _wrap({List<CheckIn> checkIns = const []}) {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'PATIENT', patientId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: Scaffold(
        body: RecentCheckInsWidget(checkIns: checkIns),
      ),
    ),
  );
}

void main() {
  group('RecentCheckInsWidget – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(RecentCheckInsWidget), findsOneWidget);
    });

    testWidgets('shows Recent Check-Ins heading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Recent Check-Ins'), findsOneWidget);
    });

    testWidgets('shows Check In button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Check In'), findsOneWidget);
    });

    testWidgets('renders with empty list without crashing', (tester) async {
      await tester.pumpWidget(_wrap(checkIns: []));
      expect(find.byType(RecentCheckInsWidget), findsOneWidget);
    });
  });
}
