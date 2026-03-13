// Tests for PatientNotesWidget from lib/widgets/patient_notes_widget.dart.
// _loadPatientNotes() called in initState — HTTP, _isLoading=true initially.
// Note: medical_notes_widget.dart also defines PatientNotesWidget (different file).

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/patient_notes_widget.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: PatientNotesWidget(
          patientId: 1,
          patientName: 'Jane Doe',
        ),
      ),
    );

void main() {
  group('PatientNotesWidget (patient_notes_widget.dart) – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(PatientNotesWidget), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Patient Notes & Documents header', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Patient Notes'), findsOneWidget);
    });
  });
}
