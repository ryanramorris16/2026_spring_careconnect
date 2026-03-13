// Tests for PatientNotesWidget from medical_notes_widget.dart
// (lib/widgets/medical_notes_widget.dart).
//
// _loadPatientNotes() called in initState — HTTP, _isLoading=true initially.
// No Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/medical_notes_widget.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: PatientNotesWidget(
          patientId: 1,
          patientName: 'Jane Doe',
        ),
      ),
    );

void main() {
  group('PatientNotesWidget (medical_notes_widget) – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(PatientNotesWidget), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
