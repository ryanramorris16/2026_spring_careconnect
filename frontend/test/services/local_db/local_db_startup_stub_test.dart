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

  group('stub returns Future<void>', () {
    test('stub returns a Future', () {
      final result = stub.initializeLocalDbOnStartup();
      expect(result, isA<Future<void>>());
    });

    test('web returns a Future', () {
      final result = web.initializeLocalDbOnStartup();
      expect(result, isA<Future<void>>());
    });

    test('stub can be called multiple times', () async {
      await stub.initializeLocalDbOnStartup();
      await stub.initializeLocalDbOnStartup();
      // No exception means success
    });

    test('web can be called multiple times', () async {
      await web.initializeLocalDbOnStartup();
      await web.initializeLocalDbOnStartup();
      // No exception means success
    });
  });
}
