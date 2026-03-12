import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/local_db_startup.dart';

import '../test_support/local_db_test_bindings.dart';

void main() {
  group('Database Initialization Tests', () {
    setUpAll(() async {
      await LocalDbTestBindings.install();
    });

    tearDownAll(LocalDbTestBindings.uninstall);

    test('initializeLocalDbOnStartup runs without throwing', () async {
      await initializeLocalDbOnStartup();
      expect(true, isTrue);
    });
  });
}
