// Tests for ProfilePictureWidget
// (lib/widgets/profile_picture_widget.dart).
//
// _loadProfileImage() called in initState — HTTP via EnhancedFileService.
// Shows loading state initially; no Provider needed.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/widgets/profile_picture_widget.dart';

Widget _wrap() => const MaterialApp(
      home: Scaffold(body: ProfilePictureWidget()),
    );

void main() {
  group('ProfilePictureWidget – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ProfilePictureWidget), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });
  });
}
