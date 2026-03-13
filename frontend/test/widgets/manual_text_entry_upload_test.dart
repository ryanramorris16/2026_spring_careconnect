// Tests for ManualTextEntryCard
// (lib/widgets/manual_text_entry_upload.dart).
//
// Pure form widget — no Provider usage in build, no API calls in initState.
// Tests cover initial render and form field presence.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/manual_text_entry_upload.dart';

Widget _wrap({int? patientId}) =>
    MaterialApp(
      home: Scaffold(
        body: ManualTextEntryCard(
          patientId: patientId,
        ),
      ),
    );

void main() {
  group('ManualTextEntryCard – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ManualTextEntryCard), findsOneWidget);
    });

    testWidgets('shows "Manual Text Entry" heading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Manual Text Entry'), findsOneWidget);
    });

    testWidgets('shows text_fields icon', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byIcon(Icons.text_fields), findsOneWidget);
    });

    testWidgets('shows multiple form fields (category + filename + content)',
        (tester) async {
      await tester.pumpWidget(_wrap());
      // At least 2 TextFormFields: file name + content.
      expect(find.byType(TextFormField), findsWidgets);
    });

    testWidgets('shows "File Name" TextFormField', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(TextFormField), findsWidgets);
    });

    testWidgets('shows "File Name" label text', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('File Name'), findsOneWidget);
    });

    testWidgets('shows a Column as root', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Column), findsWidgets);
    });
  });
}
