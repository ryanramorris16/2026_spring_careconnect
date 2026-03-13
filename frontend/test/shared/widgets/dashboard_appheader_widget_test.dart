// Tests for DashboardAppHeader (lib/shared/widgets/dashboard_appheader_widget.dart).

import 'package:care_connect_app/shared/widgets/dashboard_appheader_widget.dart';
import 'package:care_connect_app/providers/user_provider.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import '../../mock_user_provider.dart';

Widget _wrap(Widget child) {
  return ChangeNotifierProvider<UserProvider>.value(
    value: MockUserProvider(mockUser: MockUser(role: 'PATIENT')),
    child: MaterialApp(home: Scaffold(appBar: child as PreferredSizeWidget)),
  );
}

void main() {
  testWidgets('renders CARECONNECT brand text', (tester) async {
    await tester.pumpWidget(_wrap(const DashboardAppHeader(
      userName: 'Alice',
      role: 'PATIENT',
    )));
    await tester.pump();
    expect(find.text('CARECONNECT'), findsOneWidget);
  });

  testWidgets('renders welcome message with userName', (tester) async {
    await tester.pumpWidget(_wrap(const DashboardAppHeader(
      userName: 'Bob',
      role: 'PATIENT',
    )));
    await tester.pump();
    expect(find.textContaining('Welcome back Bob'), findsOneWidget);
  });

  testWidgets('renders patient mood question for PATIENT role', (tester) async {
    await tester.pumpWidget(_wrap(const DashboardAppHeader(
      userName: 'Carol',
      role: 'PATIENT',
    )));
    await tester.pump();
    expect(find.text('How are you feeling today?'), findsOneWidget);
  });

  testWidgets('renders health summary for non-PATIENT role', (tester) async {
    await tester.pumpWidget(_wrap(const DashboardAppHeader(
      userName: 'Dan',
      role: 'CAREGIVER',
    )));
    await tester.pump();
    expect(find.text("Your patients' health summary"), findsOneWidget);
  });

  testWidgets('preferredSize height is 210', (tester) async {
    const header = DashboardAppHeader(userName: 'Test', role: 'PATIENT');
    expect(header.preferredSize.height, 210);
  });
}
