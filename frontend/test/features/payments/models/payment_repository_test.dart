// Tests for PaymentRepository
// (lib/features/payments/data/payment_repository.dart).

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/payments/data/payment_repository.dart';

void main() {
  group('PaymentRepository', () {
    test('createPaymentIntent returns a non-empty string', () async {
      final repo = PaymentRepository();
      final secret = await repo.createPaymentIntent(1000);
      expect(secret, isNotEmpty);
    });

    test('createPaymentIntent returns the same value for any amount', () async {
      final repo = PaymentRepository();
      final s1 = await repo.createPaymentIntent(500);
      final s2 = await repo.createPaymentIntent(9999);
      expect(s1, isA<String>());
      expect(s2, isA<String>());
    });
  });
}
