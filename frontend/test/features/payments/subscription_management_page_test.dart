// Tests for SubscriptionManagementPage
// (lib/features/payments/presentation/pages/subscription_management_page.dart).
//
// initState calls _loadSubscriptionData() via ApiService (HTTP, try/catch).
// _isLoading starts true — spinner shown immediately.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/payments/presentation/pages/subscription_management_page.dart';

Widget _wrap() => const MaterialApp(home: SubscriptionManagementPage());

void main() {
  group('SubscriptionManagementPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(SubscriptionManagementPage), findsOneWidget);
    });

    testWidgets('shows "Subscription Management" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Subscription Management'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      // _isLoading starts true; API call is pending.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
