// Tests for DigestRaw (lib/features/usps/domain/models/digest_raw.dart).
// Minimal data class — constructor and field access only.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/features/usps/domain/models/digest_raw.dart';

void main() {
  group('DigestRaw constructor', () {
    test('stores html, cids, and receivedAt', () {
      final received = DateTime(2025, 6, 1, 8, 0);
      final raw = DigestRaw(
        html: '<html><body>test</body></html>',
        cids: {'image001': [0x89, 0x50]},
        receivedAt: received,
      );
      expect(raw.html, '<html><body>test</body></html>');
      expect(raw.cids['image001'], [0x89, 0x50]);
      expect(raw.receivedAt, received);
    });

    test('empty cids map is valid', () {
      final raw = DigestRaw(
        html: '',
        cids: {},
        receivedAt: DateTime(2025),
      );
      expect(raw.cids, isEmpty);
      expect(raw.html, '');
    });
  });
}
