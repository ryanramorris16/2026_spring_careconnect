// Tests for VisitInProgressPage
// (lib/features/evv/presentation/pages/visit_in_progress_page.dart).
//
// _loadPatientDetails() called in initState; _isLoading=true while loading.
// Timer.periodic is started in initState and cancelled in dispose().

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/presentation/pages/visit_in_progress_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'CAREGIVER', caregiverId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const VisitInProgressPage(
        patientId: 1,
        serviceType: 'Personal Care',
        locationType: 'GPS',
      ),
    ),
  );
}

void main() {
  group('VisitInProgressPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(VisitInProgressPage), findsOneWidget);
    });

    testWidgets('shows "Visit in Progress" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Visit in Progress'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
