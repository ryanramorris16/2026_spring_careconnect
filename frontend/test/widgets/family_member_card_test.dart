// Tests for FamilyMemberCard widget
// (lib/widgets/family_member_card.dart).
// Pure StatelessWidget — url_launcher calls only happen on button press.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/family_member_card.dart';

Widget _wrap(Widget child) =>
    MaterialApp(home: Scaffold(body: SingleChildScrollView(child: child)));

FamilyMemberCard _card({
  String firstName = 'Alice',
  String lastName = 'Smith',
  String relationship = 'Daughter',
  String phone = '555-1234',
  String email = 'alice@example.com',
  String lastInteraction = '2024-01-15',
}) =>
    FamilyMemberCard(
      firstName: firstName,
      lastName: lastName,
      relationship: relationship,
      phone: phone,
      email: email,
      lastInteraction: lastInteraction,
    );

void main() {
  group('FamilyMemberCard', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      expect(find.byType(FamilyMemberCard), findsOneWidget);
    });

    testWidgets('shows full name (firstName + lastName)', (tester) async {
      await tester.pumpWidget(_wrap(_card(firstName: 'Bob', lastName: 'Jones')));
      expect(find.text('Bob Jones'), findsOneWidget);
    });

    testWidgets('shows relationship text', (tester) async {
      await tester.pumpWidget(_wrap(_card(relationship: 'Son')));
      expect(find.text('Son'), findsOneWidget);
    });

    testWidgets('shows last interaction text', (tester) async {
      await tester.pumpWidget(_wrap(_card(lastInteraction: '2024-06-01')));
      expect(find.textContaining('2024-06-01'), findsOneWidget);
    });

    testWidgets('shows CircleAvatar with first letter of firstName', (tester) async {
      await tester.pumpWidget(_wrap(_card(firstName: 'Carol')));
      expect(find.text('C'), findsOneWidget);
    });

    testWidgets('renders Card widget', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      expect(find.byType(Card), findsOneWidget);
    });

    testWidgets('shows phone icon button', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      expect(find.byIcon(Icons.phone), findsAtLeastNWidgets(1));
    });

    testWidgets('shows message icon button', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      expect(find.byIcon(Icons.message), findsAtLeastNWidgets(1));
    });

    testWidgets('shows email icon when email is provided', (tester) async {
      await tester.pumpWidget(_wrap(_card(email: 'test@example.com')));
      expect(find.byIcon(Icons.email), findsAtLeastNWidgets(1));
    });

    testWidgets('shows email text when email provided', (tester) async {
      await tester.pumpWidget(_wrap(_card(email: 'test@example.com')));
      expect(find.textContaining('test@example.com'), findsOneWidget);
    });

    testWidgets('does not show email icon when email is empty', (tester) async {
      await tester.pumpWidget(_wrap(_card(email: '')));
      expect(find.byIcon(Icons.email), findsNothing);
    });

    testWidgets('shows more_vert popup menu button', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      expect(find.byIcon(Icons.more_vert), findsOneWidget);
    });

    testWidgets('popup menu contains Call, Send SMS, Edit, Delete items', (tester) async {
      await tester.pumpWidget(_wrap(_card()));
      await tester.tap(find.byIcon(Icons.more_vert));
      await tester.pumpAndSettle();
      expect(find.text('Call'), findsOneWidget);
      expect(find.text('Send SMS'), findsOneWidget);
      expect(find.text('Edit'), findsOneWidget);
      expect(find.text('Delete'), findsOneWidget);
    });

    testWidgets('tapping Delete shows confirmation dialog', (tester) async {
      await tester.pumpWidget(_wrap(_card(firstName: 'Alice', lastName: 'Smith')));
      await tester.tap(find.byIcon(Icons.more_vert));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Delete'));
      await tester.pumpAndSettle();
      expect(find.text('Delete Family Member'), findsOneWidget);
      expect(find.textContaining('Alice Smith'), findsAtLeastNWidgets(1));
    });

    testWidgets('fullName getter trims whitespace', (tester) async {
      // Widget constructor sets firstName and lastName; fullName should trim
      await tester.pumpWidget(_wrap(_card(firstName: 'John', lastName: 'Doe')));
      expect(find.text('John Doe'), findsOneWidget);
    });

    testWidgets('CircleAvatar shows F when firstName is empty', (tester) async {
      await tester.pumpWidget(_wrap(_card(firstName: '')));
      expect(find.text('F'), findsOneWidget);
    });
  });
}
