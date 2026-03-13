// Tests for AnalyticsPage
// (lib/features/analytics/analytics_page.dart).
//
// initState uses addPostFrameCallback to call fetchAnalytics().
// With patientId <= 0: fetchAnalytics() sets error and loading=false immediately
// (no HTTP call). Tests use pump() to trigger the callback.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/analytics/analytics_page.dart';

Widget _wrap({int patientId = 0}) =>
    MaterialApp(home: AnalyticsPage(patientId: patientId));

void main() {
  group('AnalyticsPage – initial render (invalid patientId)', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(patientId: 0));
      await tester.pump(); // trigger postFrameCallback
      expect(find.byType(AnalyticsPage), findsOneWidget);
    });

    testWidgets('shows "Patient Analytics" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap(patientId: 0));
      await tester.pump();
      expect(find.text('Patient Analytics'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap(patientId: 0));
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows error message for invalid patientId', (tester) async {
      await tester.pumpWidget(_wrap(patientId: 0));
      await tester.pump(); // postFrameCallback fires fetchAnalytics
      // With patientId <= 0, error is set without HTTP call.
      expect(find.textContaining('Invalid patient ID'), findsOneWidget);
    });

    testWidgets('does NOT show CircularProgressIndicator after error',
        (tester) async {
      await tester.pumpWidget(_wrap(patientId: 0));
      await tester.pump();
      // loading=false after invalid patientId short-circuits fetchAnalytics.
      expect(find.byType(CircularProgressIndicator), findsNothing);
    });
  });
}
