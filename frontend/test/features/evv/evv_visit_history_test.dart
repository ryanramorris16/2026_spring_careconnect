// Tests for EvvVisitHistoryPage
// (lib/features/evv/presentation/pages/evv_visit_history.dart).
//
// initState calls _performSearch() which sets _isLoading=true synchronously,
// then makes an API call (EvvService, try/catch).
// Search filters Card is always rendered above the results area.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/evv/presentation/pages/evv_visit_history.dart';

Widget _wrap() => const MaterialApp(home: EvvVisitHistoryPage());

void main() {
  group('EvvVisitHistoryPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(EvvVisitHistoryPage), findsOneWidget);
    });

    testWidgets('shows "EVV Visit History" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('EVV Visit History'), findsOneWidget);
    });

    testWidgets('shows "Search Filters" section', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Search Filters'), findsOneWidget);
    });

    testWidgets('shows "Search" button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Search'), findsOneWidget);
    });

    testWidgets('shows Form widget', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Form), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while searching', (tester) async {
      // _performSearch() in initState sets _isLoading=true before any await.
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
