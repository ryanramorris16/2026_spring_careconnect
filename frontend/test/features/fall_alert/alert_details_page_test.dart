// Tests for AlertDetailsPage
// (lib/features/fall_alert/pages/alert_details_page.dart).
//
// StatelessWidget — no API calls, no Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/fall_alert/pages/alert_details_page.dart';
import 'package:care_connect_app/features/fall_alert/models/fall_alert.dart';

FallAlert _makeAlert() => FallAlert(
      id: 'alert-1',
      patientId: 'patient-1',
      patientName: 'Jane Doe',
      detectedAtUtc: DateTime.utc(2025, 1, 1, 12),
      source: 'watch',
      hasLiveVideo: false,
    );

Widget _wrap() => MaterialApp(home: AlertDetailsPage(alert: _makeAlert()));

void main() {
  group('AlertDetailsPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(AlertDetailsPage), findsOneWidget);
    });

    testWidgets('shows "Fall Alert" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Fall Alert'), findsOneWidget);
    });

    testWidgets('shows patient name', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Jane Doe'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
