// Tests for AIChat from lib/widgets/ai_chat.dart.
// Pure StatelessWidget — no HTTP, no Provider.
// Shows header with 'CareConnect AI Assistant' text.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/ai_chat.dart' as basic_ai;

Widget _wrap({required String role, bool isModal = false}) => MaterialApp(
      home: Scaffold(
        body: basic_ai.AIChat(role: role, isModal: isModal),
      ),
    );

void main() {
  group('AIChat (basic ai_chat.dart) – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(role: 'patient'));
      expect(find.byType(basic_ai.AIChat), findsOneWidget);
    });

    testWidgets('shows CareConnect AI Assistant header', (tester) async {
      await tester.pumpWidget(_wrap(role: 'patient'));
      expect(find.text('CareConnect AI Assistant'), findsOneWidget);
    });

    testWidgets('shows role text in chat area', (tester) async {
      await tester.pumpWidget(_wrap(role: 'caregiver'));
      expect(find.textContaining('caregiver'), findsOneWidget);
    });
  });
}
