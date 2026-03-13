// Tests for FileManagementPage
// (lib/pages/file_management_page.dart).
//
// initState calls _loadFiles() using Provider.of<UserProvider> (API, try/catch).
// _isLoading starts true — spinner shown immediately.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:care_connect_app/pages/file_management_page.dart';
import 'package:care_connect_app/providers/user_provider.dart';

import '../mock_user_provider.dart';

Widget _wrap() {
  final provider = MockUserProvider(mockUser: MockUser(id: 1, role: 'PATIENT'));
  return MaterialApp(
    home: ChangeNotifierProvider<UserProvider>.value(
      value: provider,
      child: const FileManagementPage(),
    ),
  );
}

void main() {
  group('FileManagementPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(FileManagementPage), findsOneWidget);
    });

    testWidgets('shows "File Management" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('File Management'), findsOneWidget);
    });

    testWidgets('shows CircularProgressIndicator while loading', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
