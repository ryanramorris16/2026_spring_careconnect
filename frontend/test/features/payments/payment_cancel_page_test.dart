// Tests for PaymentCancelPage widget
// (lib/features/payments/presentation/pages/payment_cancel_page.dart).
//
// Pure StatelessWidget — no API calls or Provider usage at render time.
// Provider.of<UserProvider> is accessed only inside onPressed callbacks,
// so render-only tests work with a plain MaterialApp wrapper.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/payments/presentation/pages/payment_cancel_page.dart';

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  group('PaymentCancelPage – default (isRegistration=false)', () {
    testWidgets('renders without crashing', (tester) async {
      // Verifies the page builds without error for the standard cancel case.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.byType(PaymentCancelPage), findsOneWidget);
    });

    testWidgets('shows "Payment Cancelled" heading', (tester) async {
      // The main heading must be visible in the standard cancel case.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.text('Payment Cancelled'), findsWidgets);
    });

    testWidgets('shows cancel_outlined icon', (tester) async {
      // The large red cancel icon must be present.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.byIcon(Icons.cancel_outlined), findsOneWidget);
    });

    testWidgets('shows "no charges were made" message', (tester) async {
      // The body text should reassure the user no charges were made.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(
        find.textContaining('No charges were made'),
        findsWidgets,
      );
    });

    testWidgets('shows "Return to Dashboard" button', (tester) async {
      // The primary action for standard cancel is returning to the dashboard.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.text('Return to Dashboard'), findsOneWidget);
    });

    testWidgets('shows "Go to Home" secondary link', (tester) async {
      // The secondary TextButton lets the user navigate home.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.text('Go to Home'), findsOneWidget);
    });

    testWidgets('shows ElevatedButton', (tester) async {
      // An ElevatedButton is the primary CTA on this page.
      await tester.pumpWidget(_wrap(const PaymentCancelPage()));
      expect(find.byType(ElevatedButton), findsOneWidget);
    });
  });

  group('PaymentCancelPage – isRegistration=true', () {
    testWidgets('renders without crashing', (tester) async {
      // Verifies the registration-cancel variant builds without error.
      await tester.pumpWidget(
          _wrap(const PaymentCancelPage(isRegistration: true)));
      expect(find.byType(PaymentCancelPage), findsOneWidget);
    });

    testWidgets('shows "Try Payment Again" button for registration', (tester) async {
      // Registration cancel redirects the user to retry payment.
      await tester.pumpWidget(
          _wrap(const PaymentCancelPage(isRegistration: true)));
      expect(find.text('Try Payment Again'), findsOneWidget);
    });

    testWidgets('shows "Skip for Now" secondary link for registration',
        (tester) async {
      // Registration cancel has a "Skip" option to go to login.
      await tester.pumpWidget(
          _wrap(const PaymentCancelPage(isRegistration: true)));
      expect(find.text('Skip for Now (Go to Login)'), findsOneWidget);
    });

    testWidgets('shows registration-specific body message', (tester) async {
      // Registration cancel text explains payment is needed to complete setup.
      await tester.pumpWidget(
          _wrap(const PaymentCancelPage(isRegistration: true)));
      expect(
        find.textContaining('registration is not complete'),
        findsOneWidget,
      );
    });
  });
}
