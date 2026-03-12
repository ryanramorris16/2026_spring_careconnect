// NOTE: Native mood storage service is disabled pending Dart conditional
// compilation improvements. Both mobile and web currently use the web stub.
//
// To re-enable native sqlite3-based mood storage:
// 1. Uncomment drift, sqlite3, sqlcipher_flutter_libs in pubspec.yaml
// 2. Uncomment drift_dev in dev_dependencies
// 3. Restore app_database.dart implementation and app_database.g.dart  
// 4. Restore mood_storage_service_io.dart full implementation
// 5. Update this file to properly conditionally import based on platform
//
// import 'mood_storage_service_web.dart'
//     if (dart.library.io) 'mood_storage_service_io.dart' as mss;

// For now, always use the web stub implementation (works on all platforms)
export 'mood_storage_service_web.dart'
    show MoodStorageService, SyncSummary, MoodQueueItem, OfflinePersistenceDisabledException;
