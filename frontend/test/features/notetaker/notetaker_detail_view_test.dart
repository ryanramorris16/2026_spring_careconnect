// Tests for NotetakerDetailView
// (lib/features/notetaker/presentation/notetaker_detail_view.dart).
//
// Requires GoRouterState.of(context).extra (a PatientNote or null).
// When extra is null/non-PatientNote, addPostFrameCallback navigates to '/notetaker-search'.
// Uses UserProvider.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/features/notetaker/presentation/notetaker_detail_view.dart';
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
          child: const NotetakerDetailView(),
        ),
      ),
      GoRoute(
        path: '/notetaker-search',
        builder: (context, state) =>
            const Scaffold(body: Text('Notetaker Search')),
      ),
    ],
  );
  return MaterialApp.router(routerConfig: router);
}

void main() {
  group('NotetakerDetailView – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      // No extra provided → navigates to /notetaker-search
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('navigates to notetaker-search when extra is null', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump(); // process post-frame callback navigation
      await tester.pump(); // settle after navigation
      expect(find.text('Notetaker Search'), findsOneWidget);
    });
  });
}
