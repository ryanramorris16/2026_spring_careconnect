// Tests for AppBarHelper from lib/widgets/app_bar_helper.dart.
// Static utility — creates an AppBar with consistent styling.
// Tested by using it inside a Scaffold within a MaterialApp.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/app_bar_helper.dart';

Widget _wrap({String title = 'Test Title'}) => MaterialApp(
      home: Builder(
        builder: (context) => Scaffold(
          appBar: AppBarHelper.createAppBar(context, title: title),
          body: const SizedBox(),
        ),
      ),
    );

void main() {
  group('AppBarHelper.createAppBar', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(AppBar), findsOneWidget);
    });

    testWidgets('shows the given title', (tester) async {
      await tester.pumpWidget(_wrap(title: 'My Page'));
      expect(find.text('My Page'), findsOneWidget);
    });

    testWidgets('shows back button', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byIcon(Icons.arrow_back), findsOneWidget);
    });
  });
}
