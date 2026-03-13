// Tests for save_service_stub.dart
// (lib/features/invoices/services/excel/save_service_stub.dart).
// The stub throws UnsupportedError because file saving is not supported on
// platforms that don't match mobile (dart:io) or web (package:web).

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/invoices/services/excel/save_service_stub.dart';

void main() {
  testWidgets('saveAndOpenFile throws UnsupportedError', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: Scaffold(body: SizedBox())));
    final context = tester.element(find.byType(SizedBox));
    expect(
      () => saveAndOpenFile([1, 2, 3], 'test.xlsx', context),
      throwsA(isA<UnsupportedError>()),
    );
  });
}
