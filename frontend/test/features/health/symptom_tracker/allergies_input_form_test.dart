// Tests for AllergyInputForm
// (lib/features/health/symptom-tracker/widgets/allergies_input_form.dart).
//
// _initApi() called in initState — catches exceptions gracefully, sets _apiReady=false.
// VoiceCommandAI only used in button onPressed (not in build/initState).
// No Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/symptom-tracker/widgets/allergies_input_form.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: AllergyInputForm(
            patientId: '1',
            onAllergyAdded: (_) {},
          ),
        ),
      ),
    );

void main() {
  group('AllergyInputForm – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(AllergyInputForm), findsOneWidget);
    });

    testWidgets('shows "Add Drug Allergy" heading', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Add Drug Allergy'), findsWidgets);
    });

    testWidgets('shows Drug/Medication label', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Drug/Medication'), findsOneWidget);
    });

    testWidgets('shows Allergic Reaction label', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Allergic Reaction'), findsOneWidget);
    });

    testWidgets('shows Severity label', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Severity'), findsOneWidget);
    });

    testWidgets('shows Use AI Voice button', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Use AI Voice'), findsOneWidget);
    });
  });
}
