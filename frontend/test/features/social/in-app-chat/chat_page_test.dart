// Tests for ChatPage from
// lib/features/social/in-app-chat/pages/chat-page.dart.
// Uses flutter_chat_ui InMemoryChatController — no HTTP in initState.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/social/in-app-chat/pages/chat-page.dart';

Widget _wrap() => const MaterialApp(
      home: ChatPage(contactName: 'Dr. Smith', contactRole: 'Doctor'),
    );

void main() {
  group('ChatPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(ChatPage), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows contact name in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('Dr. Smith'), findsOneWidget);
    });
  });
}
