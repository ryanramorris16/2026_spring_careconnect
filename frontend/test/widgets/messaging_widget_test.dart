// Tests for MessagingWidget
// (lib/widgets/messaging_widget.dart).
//
// _loadMessages() called in initState via MessagingService HTTP.
// _isLoading=true initially.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/messaging_widget.dart';

Widget _wrap() => const MaterialApp(
      home: MessagingWidget(
        currentUserId: 'user1',
        currentUserName: 'Alice',
        recipientId: 'user2',
        recipientName: 'Bob',
      ),
    );

void main() {
  group('MessagingWidget – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(MessagingWidget), findsOneWidget);
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
