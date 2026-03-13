// Tests for FileUploadWidget from lib/widgets/file_upload_widget.dart.
// No HTTP in initState. Provider.of<UserProvider> only used on upload action.
// Pure UI render test — no Provider needed for initial render.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/file_upload_widget.dart';

Widget _wrap({String? customTitle}) => MaterialApp(
      home: Scaffold(
        body: FileUploadWidget(customTitle: customTitle),
      ),
    );

void main() {
  group('FileUploadWidget – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(FileUploadWidget), findsOneWidget);
    });

    testWidgets('shows Upload File header by default', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.textContaining('Upload File'), findsWidgets);
    });

    testWidgets('shows custom title when provided', (tester) async {
      await tester.pumpWidget(_wrap(customTitle: 'Upload Invoice'));
      expect(find.text('Upload Invoice'), findsOneWidget);
    });

    testWidgets('shows category selector', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Select Category'), findsOneWidget);
    });
  });
}
