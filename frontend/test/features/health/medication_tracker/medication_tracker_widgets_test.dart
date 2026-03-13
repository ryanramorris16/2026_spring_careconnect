// Tests for medication tracker widgets:
// - MedicationAppHeader (medication-header.dart) — PreferredSizeWidget
// - MedicationCard (medication-card.dart) — StatefulWidget, no HTTP in build

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/health/medication-tracker/widgets/medication-header.dart';
import 'package:care_connect_app/features/health/medication-tracker/widgets/medication-card.dart';
import 'package:care_connect_app/features/health/medication-tracker/models/medication-model.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../../mock_user_provider.dart';

Medication _makeMedication() => Medication(
      id: 1,
      medicationName: 'Aspirin',
      dosage: '100mg',
      frequency: 'Daily',
      route: 'Oral',
      isActive: true,
      status: MedicationStatus.upcoming,
    );

Widget _wrapHeader() => MaterialApp(
      home: Scaffold(
        appBar: MedicationAppHeader(onAddPressed: () {}),
        body: const SizedBox(),
      ),
    );

Widget _wrapCard() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'PATIENT', patientId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: Scaffold(
        body: MedicationCard(
          medication: _makeMedication(),
          onStatusChanged: (_) {},
        ),
      ),
    ),
  );
}

void main() {
  group('MedicationAppHeader widget', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrapHeader());
      expect(find.byType(MedicationAppHeader), findsOneWidget);
    });

    testWidgets('shows "Add" button', (tester) async {
      await tester.pumpWidget(_wrapHeader());
      expect(find.text('Add'), findsOneWidget);
    });

    testWidgets('shows add icon', (tester) async {
      await tester.pumpWidget(_wrapHeader());
      expect(find.byIcon(Icons.add), findsOneWidget);
    });
  });

  group('MedicationCard widget', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrapCard());
      expect(find.byType(MedicationCard), findsOneWidget);
    });

    testWidgets('shows medication name', (tester) async {
      await tester.pumpWidget(_wrapCard());
      expect(find.text('Aspirin'), findsOneWidget);
    });

    testWidgets('shows dosage', (tester) async {
      await tester.pumpWidget(_wrapCard());
      expect(find.textContaining('100mg'), findsOneWidget);
    });
  });
}
