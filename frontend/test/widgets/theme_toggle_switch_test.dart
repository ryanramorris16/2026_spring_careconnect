// Tests for ThemeToggleSwitch widget
// (lib/widgets/theme_toggle_switch.dart)

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:care_connect_app/widgets/theme_toggle_switch.dart';
import 'package:care_connect_app/providers/theme_provider.dart';

Widget _wrap(Widget child, ThemeProvider provider) => ChangeNotifierProvider<ThemeProvider>.value(
      value: provider,
      child: MaterialApp(home: Scaffold(body: child)),
    );

void main() {
  group('ThemeToggleSwitch', () {
    late ThemeProvider themeProvider;

    setUp(() {
      SharedPreferences.setMockInitialValues({});
      themeProvider = ThemeProvider();
    });

    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(const ThemeToggleSwitch(), themeProvider));
      await tester.pump();
      expect(find.byType(ThemeToggleSwitch), findsOneWidget);
    });

    testWidgets('shows Light Mode label by default (light theme)', (tester) async {
      await tester.pumpWidget(_wrap(const ThemeToggleSwitch(), themeProvider));
      await tester.pump();
      // System brightness in tests is light by default
      expect(find.text('Light Mode'), findsOneWidget);
    });

    testWidgets('shows icon button by default (showIcon=true)', (tester) async {
      await tester.pumpWidget(_wrap(const ThemeToggleSwitch(), themeProvider));
      await tester.pump();
      expect(find.byType(IconButton), findsOneWidget);
    });

    testWidgets('shows Switch when showIcon=false', (tester) async {
      await tester.pumpWidget(_wrap(
        const ThemeToggleSwitch(showIcon: false),
        themeProvider,
      ));
      await tester.pump();
      expect(find.byType(Switch), findsOneWidget);
      expect(find.byType(IconButton), findsNothing);
    });

    testWidgets('hides label when showLabel=false', (tester) async {
      await tester.pumpWidget(_wrap(
        const ThemeToggleSwitch(showLabel: false),
        themeProvider,
      ));
      await tester.pump();
      expect(find.text('Light Mode'), findsNothing);
      expect(find.text('Dark Mode'), findsNothing);
    });

    testWidgets('hides icon when showIcon=false and showLabel=true', (tester) async {
      await tester.pumpWidget(_wrap(
        const ThemeToggleSwitch(showIcon: false, showLabel: true),
        themeProvider,
      ));
      await tester.pump();
      expect(find.byType(IconButton), findsNothing);
      expect(find.byType(Switch), findsOneWidget);
    });

    testWidgets('shows light_mode icon in light theme', (tester) async {
      await tester.pumpWidget(_wrap(const ThemeToggleSwitch(), themeProvider));
      await tester.pump();
      expect(find.byIcon(Icons.light_mode), findsOneWidget);
    });
  });
}
