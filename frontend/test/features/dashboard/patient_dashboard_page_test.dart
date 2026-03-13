// Tests for PatientDashboard page
// (lib/features/dashboard/patient_dashboard/pages/patient_dashboard.dart).
//
// fetchPatientAndCaregivers() makes HTTP calls that fail without a server;
// loading=true on initial frame → shows CircularProgressIndicator.
// Requires UserProvider (non-null user to avoid null-cast on user?.role as String).

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/dashboard/patient_dashboard/pages/patient_dashboard.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'PATIENT', patientId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const PatientDashboard(),
    ),
  );
}

void main() {
  group('PatientDashboard page – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      tester.view.physicalSize = const Size(1600, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(PatientDashboard), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      tester.view.physicalSize = const Size(1600, 2400);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
