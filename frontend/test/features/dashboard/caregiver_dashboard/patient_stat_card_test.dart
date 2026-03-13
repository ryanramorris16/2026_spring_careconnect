// Tests for PatientStatisticsCards
// (lib/features/dashboard/caregiver-dashboard/widgets/patient-stat-card.dart).

import 'package:care_connect_app/features/dashboard/caregiver-dashboard/widgets/patient-stat-card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('renders Missed Check-Ins stat value 24', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: PatientStatisticsCards()),
      ),
    );
    // Large-screen layout: '# of Missed\nCheck-Ins'; search common substring.
    expect(find.textContaining('Missed'), findsOneWidget);
    expect(find.text('24'), findsOneWidget);
  });

  testWidgets('renders Active Patients stat value 32', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: PatientStatisticsCards()),
      ),
    );
    expect(find.text('32'), findsOneWidget);
  });

  testWidgets('renders on small screen (column layout, <600px)', (tester) async {
    tester.view.physicalSize = const Size(400, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: PatientStatisticsCards()),
      ),
    );
    expect(find.text('24'), findsOneWidget);
    expect(find.text('32'), findsOneWidget);
  });

  testWidgets('shows people_outline and monitor_heart icons', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(body: PatientStatisticsCards()),
      ),
    );
    expect(find.byIcon(Icons.people_outline), findsOneWidget);
    expect(find.byIcon(Icons.monitor_heart_outlined), findsOneWidget);
  });
}
