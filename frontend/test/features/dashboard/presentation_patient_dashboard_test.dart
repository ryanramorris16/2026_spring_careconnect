// Tests for PatientDashboard
// (lib/features/dashboard/presentation/pages/patient_dashboard.dart).
//
// With null user: fetchPatientAndCaregivers() sets error='User not logged in.'
// and loading=false synchronously (no awaits before patientId check).
// build() uses AppBarHelper (no null-cast issue) and drawer: user==null ? null : ...

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/dashboard/presentation/pages/patient_dashboard.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

Widget _wrap() {
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: _NullUserProvider(),
      child: const PatientDashboard(),
    ),
  );
}

void main() {
  group('PatientDashboard (presentation) – null user', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(PatientDashboard), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows Patient Dashboard in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Patient Dashboard'), findsOneWidget);
    });
  });
}
