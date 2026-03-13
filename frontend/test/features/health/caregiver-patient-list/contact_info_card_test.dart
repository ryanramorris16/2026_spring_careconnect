// Tests for ContactInfoCard widget
// (lib/features/health/caregiver-patient-list/widgets/contact_info_card.dart).
// Rendering-only: url_launcher calls are not triggered in tests.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/health/caregiver-patient-list/widgets/contact_info_card.dart';

Widget _wrap(Widget child) =>
    MaterialApp(home: Scaffold(body: SingleChildScrollView(child: child)));

void main() {
  group('ContactInfoCard', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.byType(ContactInfoCard), findsOneWidget);
    });

    testWidgets('shows Contact Information header', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.text('Contact Information'), findsOneWidget);
    });

    testWidgets('shows info_outline icon in header', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.byIcon(Icons.info_outline), findsOneWidget);
    });

    testWidgets('shows phone row when phone provided', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard(phone: '(555) 123-4567')));
      expect(find.text('Phone'), findsOneWidget);
      expect(find.text('(555) 123-4567'), findsOneWidget);
      expect(find.byIcon(Icons.phone), findsOneWidget);
    });

    testWidgets('hides phone row when phone is null', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.text('Phone'), findsNothing);
    });

    testWidgets('shows email row when email provided', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard(email: 'user@example.com')));
      expect(find.text('Email'), findsOneWidget);
      expect(find.text('user@example.com'), findsOneWidget);
      expect(find.byIcon(Icons.email_outlined), findsOneWidget);
    });

    testWidgets('hides email row when email is null', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.text('Email'), findsNothing);
    });

    testWidgets('shows date of birth row when provided', (tester) async {
      await tester.pumpWidget(_wrap(ContactInfoCard(
        dateOfBirth: DateTime(1990, 7, 4),
      )));
      expect(find.text('Date of Birth'), findsOneWidget);
      expect(find.text('Jul 4, 1990'), findsOneWidget);
      expect(find.byIcon(Icons.cake_outlined), findsOneWidget);
    });

    testWidgets('hides date of birth row when null', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.text('Date of Birth'), findsNothing);
    });

    testWidgets('shows address row when address provided', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard(
        addressLine1: '123 Main St',
        city: 'Springfield',
        state: 'IL',
        postalCode: '62701',
      )));
      expect(find.text('Address'), findsOneWidget);
      expect(find.textContaining('123 Main St'), findsOneWidget);
      expect(find.byIcon(Icons.home_outlined), findsOneWidget);
    });

    testWidgets('hides address row when all address fields are null', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard()));
      expect(find.text('Address'), findsNothing);
    });

    testWidgets('shows addressLine2 in address when provided', (tester) async {
      await tester.pumpWidget(_wrap(const ContactInfoCard(
        addressLine1: '456 Oak Ave',
        addressLine2: 'Apt 3B',
        city: 'Chicago',
        state: 'IL',
      )));
      expect(find.textContaining('Apt 3B'), findsOneWidget);
    });

    testWidgets('renders all fields together', (tester) async {
      await tester.pumpWidget(_wrap(ContactInfoCard(
        phone: '(555) 999-0000',
        email: 'test@test.com',
        dateOfBirth: DateTime(1985, 12, 25),
        addressLine1: '789 Pine Rd',
        city: 'Naperville',
        state: 'IL',
        postalCode: '60540',
      )));
      expect(find.text('Phone'), findsOneWidget);
      expect(find.text('Email'), findsOneWidget);
      expect(find.text('Date of Birth'), findsOneWidget);
      expect(find.text('Address'), findsOneWidget);
    });
  });
}
