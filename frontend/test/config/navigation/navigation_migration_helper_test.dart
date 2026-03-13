// Tests for NavigationMigrationHelper.shouldMigrateRoute
// (lib/config/navigation/navigation_migration_helper.dart).
//
// shouldMigrateRoute is a pure static method — no BuildContext, no Provider,
// no network I/O. It returns true for a fixed set of legacy route strings and
// false for everything else.

import 'package:flutter_test/flutter_test.dart';
import 'package:care_connect_app/config/navigation/navigation_migration_helper.dart';

void main() {
  group('NavigationMigrationHelper.shouldMigrateRoute', () {
    // --- Routes that SHOULD be migrated ---

    test('"/patient_dashboard" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/patient_dashboard'),
          isTrue);
    });

    test('"/caregiver_dashboard" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/caregiver_dashboard'),
          isTrue);
    });

    test('"/dashboard/patient" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/dashboard/patient'),
          isTrue);
    });

    test('"/dashboard/caregiver" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/dashboard/caregiver'),
          isTrue);
    });

    test('"/social_feed" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/social_feed'),
          isTrue);
    });

    test('"/social-feed" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/social-feed'),
          isTrue);
    });

    test('"/analytics" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/analytics'),
          isTrue);
    });

    test('"/profile" should migrate', () {
      expect(NavigationMigrationHelper.shouldMigrateRoute('/profile'), isTrue);
    });

    test('"/profile_settings" should migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/profile_settings'),
          isTrue);
    });

    // --- Routes that should NOT be migrated ---

    test('"/" should NOT migrate', () {
      expect(NavigationMigrationHelper.shouldMigrateRoute('/'), isFalse);
    });

    test('"/login" should NOT migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/login'), isFalse);
    });

    test('"/dashboard" should NOT migrate', () {
      // The bare /dashboard path is not in the migration list.
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/dashboard'), isFalse);
    });

    test('"/tasks" should NOT migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/tasks'), isFalse);
    });

    test('empty string should NOT migrate', () {
      expect(NavigationMigrationHelper.shouldMigrateRoute(''), isFalse);
    });

    test('arbitrary unknown route should NOT migrate', () {
      expect(
          NavigationMigrationHelper.shouldMigrateRoute('/some/random/route'),
          isFalse);
    });
  });
}
