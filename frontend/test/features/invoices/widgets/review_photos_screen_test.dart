// Tests for ReviewPhotosScreen
// (lib/features/invoices/widgets/review_photos_screen.dart).
//
// initState just copies widget.initialPhotos — no HTTP or async calls.
// Shows "Review Files" in AppBar; with empty list shows "No files yet".

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:image_picker/image_picker.dart';
import 'package:care_connect_app/features/invoices/widgets/review_photos_screen.dart';

Widget _wrap({List<XFile> photos = const []}) =>
    MaterialApp(home: ReviewPhotosScreen(initialPhotos: photos));

void main() {
  group('ReviewPhotosScreen – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(ReviewPhotosScreen), findsOneWidget);
    });

    testWidgets('shows "Review Files" in AppBar', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('Review Files'), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "No files yet" with empty photo list', (tester) async {
      await tester.pumpWidget(_wrap());
      expect(find.text('No files yet'), findsOneWidget);
    });
  });
}
