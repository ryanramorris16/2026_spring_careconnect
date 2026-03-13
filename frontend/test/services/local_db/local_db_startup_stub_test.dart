// Tests for local_db_startup_stub.dart and local_db_startup_web.dart.
// Both implement initializeLocalDbOnStartup() as a no-op async function.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/services/local_db/local_db_startup_stub.dart'
    as stub;
import 'package:care_connect_app/services/local_db/local_db_startup_web.dart'
    as web;

void main() {
  group('local_db_startup_stub.initializeLocalDbOnStartup', () {
    test('completes without error', () async {
      await expectLater(stub.initializeLocalDbOnStartup(), completes);
    });
  });

  group('local_db_startup_web.initializeLocalDbOnStartup', () {
    test('completes without error', () async {
      await expectLater(web.initializeLocalDbOnStartup(), completes);
    });
  });
}
