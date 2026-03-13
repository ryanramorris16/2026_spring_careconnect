// Tests for EmergencyContactCard widget
// (lib/features/health/caregiver-patient-list/widgets/emergency_contact_card.dart)

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/caregiver-patient-list/widgets/emergency_contact_card.dart';

Widget _wrap(Widget child) => MaterialApp(home: Scaffold(body: SingleChildScrollView(child: child)));

void main() {
  group('EmergencyContactCard', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Jane Doe',
        relationship: 'Spouse',
      )));
      expect(find.byType(EmergencyContactCard), findsOneWidget);
    });

    testWidgets('shows Emergency Contact header', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Jane Doe',
        relationship: 'Spouse',
      )));
      expect(find.text('Emergency Contact'), findsOneWidget);
    });

    testWidgets('shows contact name', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Michael Johnson',
        relationship: 'Son',
      )));
      expect(find.text('Michael Johnson'), findsOneWidget);
    });

    testWidgets('shows relationship', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Susan Lee',
        relationship: 'Daughter',
      )));
      expect(find.text('Daughter'), findsOneWidget);
    });

    testWidgets('shows phone number when provided', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Bob',
        relationship: 'Brother',
        phone: '(555) 123-4567',
      )));
      expect(find.text('(555) 123-4567'), findsOneWidget);
    });

    testWidgets('does not show phone when not provided', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Alice',
        relationship: 'Sister',
      )));
      expect(find.text('(555) 123-4567'), findsNothing);
    });

    testWidgets('shows email when provided', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Carol',
        relationship: 'Mother',
        email: 'carol@example.com',
      )));
      expect(find.text('carol@example.com'), findsOneWidget);
    });

    testWidgets('shows phone icon button', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Dan',
        relationship: 'Father',
      )));
      expect(find.byIcon(Icons.phone), findsOneWidget);
    });

    testWidgets('shows chat icon button', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Eve',
        relationship: 'Spouse',
      )));
      expect(find.byIcon(Icons.chat_bubble_outline), findsOneWidget);
    });

    testWidgets('shows contact_emergency icon in header', (tester) async {
      await tester.pumpWidget(_wrap(const EmergencyContactCard(
        contactName: 'Frank',
        relationship: 'Uncle',
      )));
      expect(find.byIcon(Icons.contact_emergency), findsOneWidget);
    });

    testWidgets('calls onCall callback when phone button tapped', (tester) async {
      var called = false;
      await tester.pumpWidget(_wrap(EmergencyContactCard(
        contactName: 'Grace',
        relationship: 'Aunt',
        onCall: () => called = true,
      )));
      await tester.tap(find.byIcon(Icons.phone));
      await tester.pump();
      expect(called, isTrue);
    });

    testWidgets('calls onMessage callback when message button tapped', (tester) async {
      var called = false;
      await tester.pumpWidget(_wrap(EmergencyContactCard(
        contactName: 'Henry',
        relationship: 'Cousin',
        onMessage: () => called = true,
      )));
      await tester.tap(find.byIcon(Icons.chat_bubble_outline));
      await tester.pump();
      expect(called, isTrue);
    });
  });
}
