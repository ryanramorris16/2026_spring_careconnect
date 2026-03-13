// Tests for VisitCompletedSuccessPage
// (lib/features/evv/presentation/pages/visit_completed_success_page.dart).
//
// _isLoading starts true; API call has try/catch.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/evv/presentation/pages/visit_completed_success_page.dart';

VisitCompletedSuccessPage _makePage() => VisitCompletedSuccessPage(
      patientId: 1,
      serviceType: 'Personal Care',
      checkinLocationType: 'GPS',
      checkoutLocationType: 'GPS',
      notes: 'Test notes',
      duration: 3600,
      checkinTime: DateTime(2025, 1, 1, 9),
      checkoutTime: DateTime(2025, 1, 1, 10),
    );

Widget _wrap() => MaterialApp(home: _makePage());

void main() {
  group('VisitCompletedSuccessPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(VisitCompletedSuccessPage), findsOneWidget);
    });

    testWidgets('shows "Visit Completed" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Visit Completed'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
