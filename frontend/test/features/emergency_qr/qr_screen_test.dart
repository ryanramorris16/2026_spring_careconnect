// Tests for QrScreen
// (lib/features/emergency_qr/qr_screen.dart).
//
// QrScreen is a StatelessWidget — no Provider, no API calls.
// Tests cover initial render with various payload configurations.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/emergency_qr/qr_screen.dart';

Widget _wrap({
  String payload = 'EMERGENCY TEST DATA',
  String? emergencyId,
  int? patientId,
}) =>
    MaterialApp(
      home: QrScreen(
        payload: payload,
        emergencyId: emergencyId,
        patientId: patientId,
      ),
    );

void main() {
  group('QrScreen – initial render (no emergencyId)', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(QrScreen), findsOneWidget);
    });

    testWidgets('shows "Emergency Information" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Emergency Information'), findsOneWidget);
    });

    testWidgets('shows a Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show PDF button when emergencyId is null',
        (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('View Emergency PDF'), findsNothing);
    });
  });

  group('QrScreen – with emergencyId', () {
    testWidgets('renders without crashing when emergencyId is provided',
        (tester) async {
      await tester.pumpWidget(_wrap(emergencyId: 'emg-123'));
      expect(find.byType(QrScreen), findsOneWidget);
    });

    testWidgets('shows AppBar with emergencyId provided', (tester) async {
      await tester.pumpWidget(_wrap(emergencyId: 'emg-123'));
      expect(find.text('Emergency Information'), findsOneWidget);
    });

    testWidgets('shows "View Emergency PDF" button when emergencyId is set',
        (tester) async {
      await tester.pumpWidget(_wrap(emergencyId: 'emg-123'));
      expect(find.text('View Emergency PDF'), findsOneWidget);
    });

    testWidgets('shows PDF icon when emergencyId is set', (tester) async {
      await tester.pumpWidget(_wrap(emergencyId: 'emg-123'));
      expect(find.byIcon(Icons.picture_as_pdf), findsOneWidget);
    });
  });
}
