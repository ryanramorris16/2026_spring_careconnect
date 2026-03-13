// Tests for EvvRecordReviewPage
// (lib/features/evv/presentation/pages/evv_record_review.dart).
//
// AppBar title is "All EVV Records (N)" where N = _filteredRecords.length.
// _isLoading starts true; _loadAllRecords() uses Provider.of<UserProvider> and
// EvvService (API, try/catch).

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/evv/presentation/pages/evv_record_review.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(mockUser: MockUser(id: 1, role: 'CAREGIVER'));
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const EvvRecordReviewPage(),
    ),
  );
}

void main() {
  group('EvvRecordReviewPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(EvvRecordReviewPage), findsOneWidget);
    });

    testWidgets('shows "All EVV Records" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('All EVV Records'), findsOneWidget);
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
