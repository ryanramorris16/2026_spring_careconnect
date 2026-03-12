import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/offline_sync_service.dart';

void main() {
  group('Offline Sync Service Tests', () {
    test('OfflineSyncService instance can be retrieved', () {
      final service = OfflineSyncService.instance();
      expect(service, isNotNull);
    });
  });
}
