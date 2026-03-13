// Tests for CommunicationWidget from lib/widgets/communication_widget.dart.
// _initializeServices() in initState: VideoCallService.initializeService()
// is COMMENTED OUT — so it just toggles _isInitializing with no side effects.
// No Provider needed for initial render.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/communication_widget.dart';

Widget _wrap() => MaterialApp(
      home: Scaffold(
        body: const CommunicationWidget(
          currentUserId: '1',
          currentUserName: 'Alice',
          targetUserId: '2',
          targetUserName: 'Bob',
        ),
      ),
    );

void main() {
  group('CommunicationWidget – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(CommunicationWidget), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows target user name', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('Bob'), findsWidgets);
    });
  });
}
