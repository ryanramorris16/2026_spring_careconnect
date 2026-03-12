import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/app_database.dart';

void main() {
  group('Database Table Tests', () {
    test('AppDatabase instance can be created', () {
      final db = AppDatabase();
      expect(db, isNotNull);
    });
  });
}
