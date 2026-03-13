// Tests for InvoiceDetailPage
// (lib/features/invoices/screens/invoice_detail_page.dart).
//
// InvoiceDetailPage is a StatefulWidget that receives an Invoice and renders
// a TabController — no API calls in initState.  Tests cover initial render,
// AppBar content, and tab navigation.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/invoices/screens/invoice_detail_page.dart';
import 'package:care_connect_app/features/invoices/models/invoice_models.dart';

// ─── Helpers ─────────────────────────────────────────────────────────────────

Invoice _makeInvoice({
  String invoiceNumber = 'INV-001',
  PaymentStatus status = PaymentStatus.pending,
}) =>
    Invoice(
      id: 'inv-1',
      invoiceNumber: invoiceNumber,
      provider: const ProviderInfo(
        name: 'Test Clinic',
        address: '1 Main St',
        phone: '555-0001',
      ),
      patient: const PatientInfo(name: 'Jane Doe'),
      dates: InvoiceDates(
        statementDate: DateTime(2025, 1, 1),
        dueDate: DateTime(2025, 2, 1),
      ),
      paymentStatus: status,
      billedToInsurance: false,
      amounts: const Amounts(
        totalCharges: 500.0,
        total: 500.0,
        amountDue: 500.0,
      ),
      paymentReferences: PaymentReferences(supportedMethods: const []),
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
      createdBy: 'admin',
      updatedBy: 'admin',
      payments: const [],
    );

Widget _wrap(Invoice invoice, {bool isNew = false}) => MaterialApp(
      home: InvoiceDetailPage(invoice: invoice, isNew: isNew),
    );

// ─── Tests ────────────────────────────────────────────────────────────────────

void main() {
  group('InvoiceDetailPage – initial render', () {
    testWidgets('renders without crashing', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.byType(InvoiceDetailPage), findsOneWidget);
    });

    testWidgets('shows invoice number in AppBar', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice(invoiceNumber: 'INV-001')));
      await tester.pump();
      expect(find.textContaining('INV-001'), findsWidgets);
    });

    testWidgets('shows "New Invoice" when isNew=true and no invoice number',
        (tester) async {
      final invoice = _makeInvoice(invoiceNumber: '');
      await tester.pumpWidget(_wrap(invoice, isNew: true));
      await tester.pump();
      expect(find.text('New Invoice'), findsOneWidget);
    });

    testWidgets('shows a TabBar', (tester) async {
      // InvoiceDetailPage uses a TabBar for navigation between sections.
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.byType(TabBar), findsOneWidget);
    });

    testWidgets('shows a TabBarView', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.byType(TabBarView), findsOneWidget);
    });

    testWidgets('shows "Details" tab', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.text('Details'), findsOneWidget);
    });

    testWidgets('shows "Services" tab', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.text('Services'), findsOneWidget);
    });

    testWidgets('shows "Payment" tab', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.text('Payment'), findsOneWidget);
    });

    testWidgets('shows Prev / Next buttons from PrevNextBar', (tester) async {
      // PrevNextBar renders Prev (OutlinedButton) and Next (FilledButton).
      await tester.pumpWidget(_wrap(_makeInvoice()));
      await tester.pump();
      expect(find.text('Prev'), findsOneWidget);
      expect(find.text('Next'), findsOneWidget);
    });
  });

  group('InvoiceDetailPage – different payment statuses', () {
    testWidgets('renders with pending status without crashing', (tester) async {
      await tester.pumpWidget(
          _wrap(_makeInvoice(status: PaymentStatus.pending)));
      await tester.pump();
      expect(find.byType(InvoiceDetailPage), findsOneWidget);
    });

    testWidgets('renders with paid status without crashing', (tester) async {
      await tester.pumpWidget(_wrap(_makeInvoice(status: PaymentStatus.paid)));
      await tester.pump();
      expect(find.byType(InvoiceDetailPage), findsOneWidget);
    });

    testWidgets('renders with overdue status without crashing', (tester) async {
      await tester.pumpWidget(
          _wrap(_makeInvoice(status: PaymentStatus.overdue)));
      await tester.pump();
      expect(find.byType(InvoiceDetailPage), findsOneWidget);
    });
  });
}
