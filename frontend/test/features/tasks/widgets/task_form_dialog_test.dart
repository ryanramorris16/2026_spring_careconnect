// Tests for TaskFormDialog
// (lib/features/tasks/presentation/widgets/task_form_dialog.dart).
//
// Pure form widget — no HTTP in initState.
// Requires TaskTypeManager (ChangeNotifierProvider) backed by SharedPreferences mock.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:care_connect_app/features/tasks/presentation/widgets/task_form_dialog.dart';
import 'package:care_connect_app/features/tasks/utils/task_type_manager.dart';

Widget _wrap({bool isCaregiver = false}) {
  return MaterialApp(
    home: Scaffold(
      body: ChangeNotifierProvider<TaskTypeManager>(
        create: (_) => TaskTypeManager(),
        child: SingleChildScrollView(
          child: TaskFormDialog(
            isCaregiver: isCaregiver,
            patients: const [],
          ),
        ),
      ),
    ),
  );
}

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('TaskFormDialog – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(TaskFormDialog), findsOneWidget);
    });

    testWidgets('shows title text field', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(TextFormField), findsWidgets);
    });

    testWidgets('shows Save button', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Save'), findsOneWidget);
    });

    testWidgets('shows Cancel button', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.text('Cancel'), findsOneWidget);
    });
  });
}
