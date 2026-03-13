// Tests for CaregiverDashboard
// (lib/features/dashboard/presentation/pages/caregiver_dashboard.dart).
//
// With null user: build() returns CircularProgressIndicator and navigates to /login.
// Requires GoRouter (context.go('/login')) and UserProvider.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/dashboard/presentation/pages/caregiver_dashboard.dart';
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
          child: const CaregiverDashboard(),
        ),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const Scaffold(body: Text('Login')),
      ),
    ],
  );
  return MaterialApp.router(routerConfig: router);
}

void main() {
  group('CaregiverDashboard (presentation) – null user', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator for null user', (tester) async {
      await tester.pumpWidget(_wrap());
      // user == null → Scaffold with CircularProgressIndicator
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('navigates to login when user is null', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump(); // process microtask navigation
      await tester.pump(); // settle
      expect(find.text('Login'), findsOneWidget);
    });
  });
}
