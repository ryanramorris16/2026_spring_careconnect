// Tests for NotetakerSearchPage
// (lib/features/notetaker/presentation/notetaker_search.dart).
//
// NotetakerSearchPage reads UserProvider in init() (called from initState).
// For a PATIENT user: _fetchPatientData() (HTTP) is called in the finally
// block — isLoading=true keeps the spinner visible on initial render.
// Tests use pump() only (NOT pumpAndSettle) to avoid advancing the HTTP await.
//
// Null-user path calls context.go('/login') via Future.microtask which
// requires GoRouter — those paths are not exercised here.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/notetaker/presentation/notetaker_search.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../../mock_user_provider.dart';

// ─── Helpers ─────────────────────────────────────────────────────────────────

Widget _wrap({String role = 'PATIENT'}) {
  final provider = MockUserProvider(
    mockUser: MockUser(id: 1, role: role, patientId: 1),
  );
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const NotetakerSearchPage(),
    ),
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

void main() {
  group('NotetakerSearchPage – initial loading state (patient user)', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      // Do NOT pumpAndSettle — HTTP call in _fetchPatientData is in flight.
      expect(find.byType(NotetakerSearchPage), findsOneWidget);
    });

    testWidgets('shows "Notetaker Assistant" in the AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Notetaker Assistant'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows a Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('does NOT show a ListView while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ListView), findsNothing);
    });
  });
}
