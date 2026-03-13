// Tests for VisitCompletePage
// (lib/features/evv/presentation/pages/visit_complete_page.dart).
//
// _loadPatientDetails() called in initState; _isLoading=true while loading.
// Uses Provider.of<UserProvider>.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/presentation/pages/visit_complete_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'CAREGIVER', caregiverId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const VisitCompletePage(
        patientId: 1,
        serviceType: 'Personal Care',
        checkinLocationType: 'GPS',
        checkoutLocationType: 'GPS',
        notes: 'Test notes',
        duration: 3600,
      ),
    ),
  );
}

void main() {
  group('VisitCompletePage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(VisitCompletePage), findsOneWidget);
    });

    testWidgets('shows "Visit Complete" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Visit Complete'), findsOneWidget);
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
