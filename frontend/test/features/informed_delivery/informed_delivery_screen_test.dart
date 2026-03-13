// Tests for InformedDeliveryScreen
// (lib/features/informed_delivery/informed_delivery_screen.dart).
//
// _loadDigestData() called in initState — async HTTP but no Provider needed.
// Uses CommonDrawer which needs UserProvider.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/informed_delivery/informed_delivery_screen.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

Widget _wrap() {
  final provider = _NullUserProvider();
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const InformedDeliveryScreen(),
    ),
  );
}

void main() {
  group('InformedDeliveryScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(InformedDeliveryScreen), findsOneWidget);
    });

    testWidgets('shows "Informed Delivery" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('Informed Delivery'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
