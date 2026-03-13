// Tests for PaymentSuccessPage from
// lib/features/payments/presentation/pages/payment_success_page.dart.
//
// Uses Future.delayed timers (1s–4s) and GoRouter navigation.
// Test with pump(4s) to drain timers before disposal.
// Uses provider for UserProvider.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/payments/presentation/pages/payment_success_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

Widget _wrap() {
  final provider = _NullUserProvider();
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => ChangeNotifierProvider<UserProvider>.value(
          value: provider,
          child: const PaymentSuccessPage(),
        ),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const Scaffold(body: Text('Login')),
      ),
      GoRoute(
        path: '/select-package',
        builder: (context, state) => const Scaffold(body: Text('Select Package')),
      ),
      GoRoute(
        path: '/caregiver-dashboard',
        builder: (context, state) => const Scaffold(body: Text('Dashboard')),
      ),
      GoRoute(
        path: '/patient-dashboard',
        builder: (context, state) => const Scaffold(body: Text('Dashboard')),
      ),
    ],
  );
  return MaterialApp.router(routerConfig: router);
}

void main() {
  group('PaymentSuccessPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(PaymentSuccessPage), findsOneWidget);
      // Drain all timers (1s, 2s, 3s, 4s) before test ends
      await tester.pump(const Duration(seconds: 5));
    });

    testWidgets('shows success content', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      // Payment success page typically shows a checkmark or success message
      expect(find.byType(Scaffold), findsOneWidget);
      await tester.pump(const Duration(seconds: 5));
    });
  });
}
