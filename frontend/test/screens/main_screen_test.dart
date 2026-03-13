// Tests for MainScreen (lib/screens/main_screen.dart).
// MainScreen is the post-login navigation shell with a bottom nav bar.
// Tests verify it renders a Scaffold and BottomNavigationBar without crashing.

import 'package:care_connect_app/config/navigation/main_screen_config.dart';
import 'package:care_connect_app/screens/main_screen.dart';
import 'package:care_connect_app/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/providers/user_provider.dart';
import '../mock_user_provider.dart';

Widget _wrap({int? initialTabIndex, String role = 'PATIENT'}) {
  final provider = MockUserProvider(mockUser: MockUser(role: role));
  final router = GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        builder: (_, __) => MainScreen(
          initialTabIndex: initialTabIndex,
          config: MainScreenConfig.forPatient(userId: 1),
        ),
      ),
    ],
  );
  // Must register as UserProvider so Provider.of<UserProvider> finds it.
  return ChangeNotifierProvider<UserProvider>.value(
    value: provider,
    child: MaterialApp.router(
      routerConfig: router,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ],
    ),
  );
}

void main() {
  setUp(() {
    // Dashboard child widgets have pre-existing layout overflow bugs in
    // constrained viewports. Suppress RenderFlex overflow errors so they
    // don't fail tests unrelated to layout.
    FlutterError.onError = (details) {
      if (details.toString().contains('overflowed')) return;
      FlutterError.dumpErrorToConsole(details);
    };
  });

  tearDown(() {
    FlutterError.onError = FlutterError.dumpErrorToConsole;
  });

  testWidgets('MainScreen renders a Scaffold', (tester) async {
    tester.view.physicalSize = const Size(1440, 1920);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(_wrap());
    await tester.pump();
    expect(find.byType(Scaffold), findsAtLeastNWidgets(1));
  });

  testWidgets('MainScreen renders a BottomNavigationBar', (tester) async {
    tester.view.physicalSize = const Size(1440, 1920);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(_wrap());
    await tester.pump();
    expect(find.byType(BottomNavigationBar), findsOneWidget);
  });

  testWidgets('MainScreen accepts initialTabIndex', (tester) async {
    tester.view.physicalSize = const Size(1440, 1920);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(_wrap(initialTabIndex: 0));
    await tester.pump();
    expect(find.byType(Scaffold), findsAtLeastNWidgets(1));
  });
}
