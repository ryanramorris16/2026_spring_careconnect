// Tests for UploadInvoicePage
// (lib/features/invoices/screens/upload_invoice.dart).
//
// _watchConnectivity() uses Connectivity plugin — in tests, platform channels
// return defaults (no exception). offline=false initially.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/invoices/screens/upload_invoice.dart';

Widget _wrap() => const MaterialApp(home: UploadInvoicePage());

void main() {
  group('UploadInvoicePage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(UploadInvoicePage), findsOneWidget);
    });

    testWidgets('shows Scaffold', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('shows "Upload File" button', (tester) async {
      await tester.pumpWidget(_wrap());
      await tester.pump();
      expect(find.textContaining('Upload'), findsWidgets);
    });
  });
}
