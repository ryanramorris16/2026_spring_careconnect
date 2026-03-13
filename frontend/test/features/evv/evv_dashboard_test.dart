// Tests for EvvDashboard
// (lib/features/evv/presentation/pages/evv_dashboard.dart).
//
// initState calls _loadDashboardData() which uses EvvService (API, try/catch).
// _isLoading starts true, so a spinner is shown immediately.
// Tests wrap with MockUserProvider to satisfy Provider.of<UserProvider>.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/presentation/pages/evv_dashboard.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap({String role = 'CAREGIVER'}) {
  final provider = MockUserProvider(mockUser: MockUser(id: 1, role: role));
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const EvvDashboard(),
    ),
  );
}

void main() {
  group('EvvDashboard – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(EvvDashboard), findsOneWidget);
    });

    testWidgets('shows "EVV Dashboard" in the AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('EVV Dashboard'), findsOneWidget);
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
