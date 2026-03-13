// Tests for AddMedicationModal from
// lib/features/health/medication-tracker/widgets/medication-add-input-form.dart.
// Pure form widget — no HTTP in initState, Provider only in action handler.
// Needs wide viewport (700px) to avoid Row overflow in header.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/medication-tracker/widgets/medication-add-input-form.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: AddMedicationModal(
          onMedicationAdded: (_) {},
        ),
      ),
    );

void main() {
  setUp(() {
    // Wide viewport to avoid Row overflow in the modal header
  });

  group('AddMedicationModal – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      tester.view.physicalSize = const Size(700, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_wrap());
      expect(find.byType(AddMedicationModal), findsOneWidget);
    });

    testWidgets('shows Add New Medication title', (tester) async {
      tester.view.physicalSize = const Size(700, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_wrap());
      expect(find.text('Add New Medication'), findsOneWidget);
    });

    testWidgets('shows form fields', (tester) async {
      tester.view.physicalSize = const Size(700, 1600);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_wrap());
      expect(find.byType(TextFormField), findsWidgets);
    });
  });
}
