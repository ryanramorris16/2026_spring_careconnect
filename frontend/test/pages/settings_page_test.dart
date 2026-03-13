// Tests for SettingsPage from lib/pages/settings_page.dart.
//
// Uses UserProvider (null user), LocaleProvider, AppLocalizations.
// _loadNotificationSettings() checks user != null — skips HTTP when null.
// _loadTelemetrySettings() reads SharedPreferences — runs safely in tests.
// May show telemetry dialog; pumpAndSettle() handles it.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/pages/settings_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';
import 'package:care_connect_app/providers/locale_provider.dart';
import 'package:care_connect_app/providers/theme_provider.dart';
import 'package:care_connect_app/l10n/app_localizations.dart';

import '../mock_user_provider.dart';

class _NullUserProvider extends MockUserProvider {
  _NullUserProvider() : super(mockUser: null);

  @override
  UserSession? get user => null;
}

Widget _wrap() {
  return MultiProvider(
    providers: [
      ChangeNotifierProvider<UserProvider>.value(value: _NullUserProvider()),
      ChangeNotifierProvider<LocaleProvider>(create: (_) => LocaleProvider()),
      ChangeNotifierProvider<ThemeProvider>(create: (_) => ThemeProvider()),
    ],
    child: MaterialApp(
      locale: const Locale('en'),
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: const SettingsPage(),
    ),
  );
}

void main() {
  group('SettingsPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(SettingsPage), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows Settings title', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Settings'), findsWidgets);
    });
  });
}
