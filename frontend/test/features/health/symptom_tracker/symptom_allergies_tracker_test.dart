// Tests for SymptomsAllergiesPage
// (lib/features/health/symptom-tracker/pages/symptom_allergies_tracker_screen.dart).
//
// initState calls _resolvePatientId() using Provider.of<UserProvider>.
// With patientId=null, returns early (no HTTP) with error message.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/health/symptom-tracker/pages/symptom_allergies_tracker_screen.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'PATIENT', patientId: null),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const SymptomsAllergiesPage(),
    ),
  );
}

void main() {
  group('SymptomsAllergiesPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(SymptomsAllergiesPage), findsOneWidget);
    });

    testWidgets('shows "Symptoms & Allergies" heading', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Symptoms & Allergies'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows error for null patientId', (tester) async {
      // _resolvePatientId() sets _errorMessage without HTTP when patientId is null.
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('Patient ID not found'), findsOneWidget);
    });
  });
}
