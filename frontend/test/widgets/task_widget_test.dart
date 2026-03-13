// Tests for TaskFormDialog and TaskInfo from lib/widgets/task_widget.dart.
//
// TaskFormDialog with existingTask=null: calls _fetchTemplates() in initState
//   → loadingTemplates=true → shows CircularProgressIndicator.
// TaskInfo: pure StatelessWidget showing an AlertDialog.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/task_widget.dart';
import 'package:care_connect_app/features/tasks/models/task_model.dart';

Widget _wrapFormDialog({Task? existingTask}) => MaterialApp(
      home: Scaffold(
        body: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () => showDialog(
              context: context,
              builder: (_) => TaskFormDialog(
                patientId: 1,
                existingTask: existingTask,
              ),
            ),
            child: const Text('Open'),
          ),
        ),
      ),
    );

Task _makeTask() => Task(
      id: 1,
      name: 'Walk patient',
      description: 'Daily morning walk',
      date: DateTime(2025, 6, 1),
      timeOfDay: const TimeOfDay(hour: 9, minute: 0),
      assignedPatientId: 1,
      isComplete: false,
      notifications: null,
      frequency: 'DAILY',
      interval: 1,
      count: null,
      daysOfWeek: List<bool>.filled(7, false),
    );

Widget _wrapTaskInfo() => MaterialApp(
      home: Scaffold(
        body: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () => showDialog(
              context: context,
              builder: (_) => TaskInfo(task: _makeTask()),
            ),
            child: const Text('Open Info'),
          ),
        ),
      ),
    );

void main() {
  group('TaskFormDialog – new task (template selection state)', () {
    // _fetchTemplates has .timeout(30s) — pump(31s) to drain the pending timer.
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrapFormDialog());
      await tester.tap(find.text('Open'));
      await tester.pump();
      expect(find.byType(TaskFormDialog), findsOneWidget);
      await tester.pump(const Duration(seconds: 31));
    });

    testWidgets('shows loading indicator while fetching templates', (
      tester,
    ) async {
      await tester.pumpWidget(_wrapFormDialog());
      await tester.tap(find.text('Open'));
      await tester.pump();
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      await tester.pump(const Duration(seconds: 31));
    });

    testWidgets('shows Assign Task title', (tester) async {
      await tester.pumpWidget(_wrapFormDialog());
      await tester.tap(find.text('Open'));
      await tester.pump();
      expect(find.text('Assign Task'), findsOneWidget);
      await tester.pump(const Duration(seconds: 31));
    });
  });

  group('TaskFormDialog – edit existing task', () {
    testWidgets('shows Edit Task form when existingTask provided', (
      tester,
    ) async {
      await tester.pumpWidget(_wrapFormDialog(existingTask: _makeTask()));
      await tester.tap(find.text('Open'));
      await tester.pump();
      expect(find.text('Edit Task'), findsOneWidget);
    });
  });

  group('TaskInfo', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrapTaskInfo());
      await tester.tap(find.text('Open Info'));
      await tester.pump();
      expect(find.byType(TaskInfo), findsOneWidget);
    });

    testWidgets('shows task name in dialog title', (tester) async {
      await tester.pumpWidget(_wrapTaskInfo());
      await tester.tap(find.text('Open Info'));
      await tester.pump();
      expect(find.text('Walk patient'), findsOneWidget);
    });
  });
}
