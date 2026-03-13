// Tests for CheckoutLocationPage
// (lib/features/evv/presentation/pages/checkout_location_page.dart).
//
// _loadPatientDetails() called in initState; uses Provider.of<UserProvider>.
// Non-null CAREGIVER user avoids the "User not authenticated" exception.
// _isLoading=true while loading.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/presentation/pages/checkout_location_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: 'CAREGIVER', caregiverId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const CheckoutLocationPage(
        patientId: 1,
        serviceType: 'Personal Care',
        locationType: 'HOME',
        notes: '',
        duration: 3600,
      ),
    ),
  );
}

void main() {
  group('CheckoutLocationPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CheckoutLocationPage), findsOneWidget);
    });

    testWidgets('shows Check-Out Location in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Check-Out'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
