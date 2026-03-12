import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/offline_sync_row.dart';

void main() {
  group('OfflineSyncDbRow Tests', () {
    test('OfflineSyncDbRow stores values correctly', () {
      final row = OfflineSyncDbRow(
        id: '1',
        fingerprint: 'abc123',
        method: 'POST',
        url: '/api/mood',
        headersJson: '{"Content-Type":"application/json"}',
        bodyJson: '{"mood":"happy"}',
        createdAt: DateTime(2024, 1, 1),
        status: 'pending',
        retryCount: 0,
        lastError: null,
      );

      expect(row.id, '1');
      expect(row.method, 'POST');
      expect(row.url, '/api/mood');
      expect(row.status, 'pending');
      expect(row.retryCount, 0);
    });
  });
}
