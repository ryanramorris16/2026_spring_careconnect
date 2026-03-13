// Tests for FileUploadDemo from lib/widgets/file_upload_demo.dart.
// _loadFiles() called in initState — HTTP, _isLoading=true initially.
// Contains FileUploadWidget which does NOT need Provider for initial render.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/file_upload_demo.dart';

Widget _wrap() => const MaterialApp(home: FileUploadDemo());

void main() {
  group('FileUploadDemo – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(FileUploadDemo), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows File Upload Demo AppBar title', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('File Upload Demo'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });
  });
}
