import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart';

// IMPORTANT: Drift imports are disabled on web
// The entire AppDatabase class is native-only and should never be used on web
// 
// On web platforms:
// - mood_storage_service.dart imports mood_storage_service_web.dart instead
// - local_db_startup.dart calls the web no-op implementation
//
// DO NOT uncomment drift imports unless you also:
// 1. Re-enable drift in pubspec.yaml
// 2. Re-enable sqlite3 and sqlcipher_flutter_libs in pubspec.yaml  
// 3. Re-enable drift_dev in dev_dependencies
// import 'package:drift/drift.dart';
// import 'package:drift/native.dart' as drift_native;

import 'db_encryption_service.dart';
// import 'generated/jpa_drift_bundle.dart';

// Conditional import of generated drift bundle
// import 'generated/jpa_drift_bundle_web.dart'
//     if (dart.library.io) 'generated/jpa_drift_bundle.dart' as drift_bundle;

// part 'app_database.g.dart';

/// DISABLED ON WEB: This class is native-only and requires SQLite/Drift.
/// 
/// Encrypted Drift database used by mobile offline storage on iOS, Android,
/// Windows, macOS, and Linux platforms only.
/// 
/// NOTE: This implementation should never be instantiated or imported on web.
/// Conditional imports in mood_storage_service.dart and local_db_startup.dart
/// ensure this file is not loaded on web.
///
/// Table definitions are generated from backend JPA models.
/// 
/// To use this class (on native platforms):
/// 1. Uncomment the imports above
/// 2. Re-enable drift, sqlite3, and sqlcipher in pubspec.yaml
/// 3. Run: dart run tool/generate_sql_from_jpa.dart
class AppDatabase {
  // PLACEHOLDER: Class disabled on web - only uncomment when drift is re-enabled
  // Original implementation would extend _$AppDatabase
  
  AppDatabase({/* parameters */}) {
    throw UnsupportedError(
      'AppDatabase (Drift SQLite) is not supported on web. '
      'Use web local storage strategies (LocalStorage, IndexedDB, etc) instead.',
    );
  }

  // ORIGINAL METHODS - COMMENTED OUT UNTIL DRIFT IS RE-ENABLED
  // 
  // Future<bool> isEncrypted() async { ... }
  // Future<int> insertMood({...}) { ... }
  // Future<List<Mood>> getMoodsForUser(int userIdValue) { ... }
  // Future<List<Mood>> getMoodsForUserOldestFirst(int userIdValue) { ... }  
  // Future<Mood?> getMoodByIdForUser({...}) { ... }
  // Future<int> deleteMoodById(int moodId) { ... }
  // Future<void> deleteMoodsByIds(Iterable<int> ids) async { ... }
}

// ORIGINAL CONNECTION CODE - COMMENTED OUT UNTIL DRIFT IS RE-ENABLED
// 
// QueryExecutor _openConnection(DbEncryptionService encryptionService) { ... }
