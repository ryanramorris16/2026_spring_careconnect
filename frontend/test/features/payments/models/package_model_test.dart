// Tests for PackageModel
// (lib/features/payments/models/package_model.dart).

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/payments/models/package_model.dart';

void main() {
  group('PackageModel', () {
    test('constructor stores all fields', () {
      final model = PackageModel(
        name: 'Premium Plan',
        description: 'Full access to all features',
        priceCents: 999,
        id: 'pkg-1',
      );

      expect(model.name, 'Premium Plan');
      expect(model.description, 'Full access to all features');
      expect(model.priceCents, 999);
      expect(model.id, 'pkg-1');
    });

    test('priceCents stores zero correctly', () {
      final model = PackageModel(
        name: 'Free Plan',
        description: 'Basic features',
        priceCents: 0,
        id: 'pkg-free',
      );
      expect(model.priceCents, 0);
    });

    test('priceCents stores large value correctly', () {
      final model = PackageModel(
        name: 'Enterprise Plan',
        description: 'All enterprise features',
        priceCents: 99900,
        id: 'pkg-enterprise',
      );
      expect(model.priceCents, 99900);
    });
  });
}
