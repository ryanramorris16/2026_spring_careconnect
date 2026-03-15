import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/app_database.dart'
    as local_db;
import 'package:care_connect_app/services/local_db/local_db_startup_io.dart'
    as startup_io;

import 'local_db_test_bindings.dart';

void main() {
  group('local_db_startup_io', () {
    setUpAll(LocalDbTestBindings.install);
    tearDownAll(LocalDbTestBindings.uninstall);

    setUp(LocalDbTestBindings.reset);

    test('initializes table and can be called more than once', () async {
      await startup_io.initializeLocalDbOnStartup();
      await startup_io.initializeLocalDbOnStartup();

      final db = local_db.AppDatabase();
      expect(await db.getPendingOfflineSyncCount(), 0);
      expect(await File(LocalDbTestBindings.dbPath).exists(), isTrue);

      await db.closeDb();
    });
  });
}
