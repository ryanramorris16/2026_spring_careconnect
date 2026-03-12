import 'connectivity_router_service.dart';

/// Web stub for mood storage service.
///
/// Provides no-op implementations for mood storage operations on web platform
/// since SQLite/Drift is not available. Web storage strategy can be implemented
/// here later (e.g., using IndexedDB, LocalStorage, or Hive Web).

/// Thrown when a write requires offline persistence but user disabled it.
class OfflinePersistenceDisabledException implements Exception {
  const OfflinePersistenceDisabledException(this.message);
  final String message;

  @override
  String toString() => message;
}

class SyncSummary {
  const SyncSummary({
    required this.attempted,
    required this.created,
    required this.skippedExisting,
    required this.failed,
    required this.ranOnline,
  });

  final int attempted;
  final int created;
  final int skippedExisting;
  final int failed;
  final bool ranOnline;
}

class MoodQueueItem {
  const MoodQueueItem({
    required this.localId,
    required this.score,
    required this.label,
    required this.createdAt,
  });

  final int localId;
  final int score;
  final String label;
  final DateTime createdAt;
}

/// Web stub - no-op implementation for mood storage.
/// 
/// All methods return empty/no-op values suitable for web where local
/// SQLite storage is not available.
class MoodStorageService {
  const MoodStorageService({
    ConnectivityRouterService? connectivityRouter,
    dynamic appDatabase,
    Future<int?> Function()? currentUserIdProvider,
    Future<bool> Function()? canUseOfflineFallback,
  });

  /// Saves mood online only (no offline fallback on web).
  Future<void> saveMood({
    required int userId,
    required int score,
    required String label,
  }) async {
    // No-op on web
  }

  /// Saves mood for the current user resolved from shared app state.
  Future<void> saveMoodForCurrentUser({
    required int score,
    required String label,
  }) async {
    // No-op on web
  }

  /// Returns mood history from backend when online, empty list offline.
  Future<List<Map<String, dynamic>>> getMoodHistory(int userId) async {
    return [];
  }

  /// Syncs local moods to backend when online (no-op on web).
  Future<SyncSummary> syncMoodsToBackendIfOnline({
    required int userId,
  }) async {
    return const SyncSummary(
      attempted: 0,
      created: 0,
      skippedExisting: 0,
      failed: 0,
      ranOnline: false,
    );
  }

  /// Returns pending local moods queue (always empty on web).
  Future<List<MoodQueueItem>> getPendingMoodQueue({
    required int userId,
  }) async {
    return [];
  }

  /// Removes one queued mood entry (no-op on web).
  Future<bool> deleteQueuedMoodItem({
    required int localId,
  }) async {
    return false;
  }

  /// Syncs one queued mood entry by local id (no-op on web).
  Future<bool> syncQueuedMoodItemById({
    required int userId,
    required int localId,
  }) async {
    return false;
  }

  /// Closes the database connection (no-op on web).
  Future<void> close() async {
    // No-op on web
  }
}
