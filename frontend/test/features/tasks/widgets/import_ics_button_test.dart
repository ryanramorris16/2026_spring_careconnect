// Tests for ImportIcsButton widget
// (lib/features/tasks/presentation/widgets/import_ics_button.dart).
//
// ImportIcsButton is a StatefulWidget whose build method only checks
// MediaQuery screen width.  API calls and file picking only happen inside
// _pickAndImportFile(), which is invoked from the dialog — not during render.
// Render-only tests are safe to run without network or platform channels.
//
// Wide screen (≥ 500 px) → ElevatedButton.icon with "Import ICS" label.
// Compact screen (< 500 px) → IconButton with tooltip "Import ICS".

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/tasks/presentation/widgets/import_ics_button.dart';

Widget _wrap(Widget child, {double width = 800}) => MaterialApp(
      home: MediaQuery(
        data: MediaQueryData(size: Size(width, 800)),
        child: Scaffold(
          body: SizedBox(width: width, child: child),
        ),
      ),
    );

const _patients = {1: 'Alice', 2: 'Bob'};

void main() {
  group('ImportIcsButton – wide screen (≥ 500 px)', () {
    testWidgets('renders without crashing', (tester) async {
      // Verifies the widget builds without error on a wide screen.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients)));
      expect(find.byType(ImportIcsButton), findsOneWidget);
    });

    testWidgets('shows "Import ICS" label on wide screen', (tester) async {
      // Wide screens should show the full labelled ElevatedButton.icon.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients)));
      expect(find.text('Import ICS'), findsOneWidget);
    });

    testWidgets('shows ElevatedButton on wide screen', (tester) async {
      // Wide screens use ElevatedButton (not a plain IconButton).
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients)));
      expect(find.byType(ElevatedButton), findsOneWidget);
    });

    testWidgets('shows file_upload icon on wide screen', (tester) async {
      // The ElevatedButton.icon includes Icons.file_upload.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients)));
      expect(find.byIcon(Icons.file_upload), findsOneWidget);
    });

    testWidgets('does NOT show IconButton on wide screen', (tester) async {
      // On wide screens the compact icon-only variant must not appear.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients)));
      expect(find.byType(IconButton), findsNothing);
    });
  });

  group('ImportIcsButton – compact screen (< 500 px)', () {
    testWidgets('renders without crashing on compact screen', (tester) async {
      // Verifies the widget builds on a phone-width screen.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients), width: 400));
      expect(find.byType(ImportIcsButton), findsOneWidget);
    });

    testWidgets('shows IconButton on compact screen', (tester) async {
      // Compact screens should show only an icon button to save space.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients), width: 400));
      expect(find.byType(IconButton), findsOneWidget);
    });

    testWidgets('does NOT show "Import ICS" label on compact screen',
        (tester) async {
      // The text label must not appear on compact screens.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients), width: 400));
      expect(find.text('Import ICS'), findsNothing);
    });

    testWidgets('shows file_upload icon on compact screen', (tester) async {
      // The icon-only variant still shows Icons.file_upload.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients), width: 400));
      expect(find.byIcon(Icons.file_upload), findsOneWidget);
    });

    testWidgets('does NOT show ElevatedButton on compact screen',
        (tester) async {
      // On compact screens the wide labelled variant must not appear.
      await tester.pumpWidget(
          _wrap(const ImportIcsButton(patientNames: _patients), width: 400));
      expect(find.byType(ElevatedButton), findsNothing);
    });
  });
}
