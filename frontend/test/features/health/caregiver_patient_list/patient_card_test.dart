// Tests for PatientCard widget
// (lib/features/health/caregiver-patient-list/widgets/patient-info-card.dart).
//
// Pure StatelessWidget with no Provider or HTTP.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/caregiver-patient-list/widgets/patient-info-card.dart';
import 'package:care_connect_app/features/health/caregiver-patient-list/models/patient-info.dart';

Patient _makePatient() => Patient(
      id: 'p-1',
      firstName: 'Sarah',
      lastName: 'Johnson',
      lastUpdated: DateTime(2025, 1, 1),
      statusMessage: 'Feeling good today',
      nextCheckIn: DateTime(2025, 1, 2, 10),
      mood: 'Good',
      moodEmoji: '😊',
      isUrgent: false,
      messageCount: 2,
    );

Widget _wrap() => MaterialApp(
      home: Scaffold(body: PatientCard(patient: _makePatient())),
    );

void main() {
  group('PatientCard widget', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(PatientCard), findsOneWidget);
    });

    testWidgets('shows patient first name', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Sarah'), findsOneWidget);
    });

    testWidgets('shows patient last name', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Johnson'), findsOneWidget);
    });

    testWidgets('shows mood emoji', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('😊'), findsWidgets);
    });
  });
}
