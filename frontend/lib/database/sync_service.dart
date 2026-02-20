import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'database.dart';

/// Sync service to push PENDING mood logs to backend (BNS-5)
class SyncService {
  static final SyncService _instance = SyncService._internal();
  factory SyncService() => _instance;
  SyncService._internal();

  final AppDatabase _database = AppDatabase();
  Timer? _syncTimer;
  bool _isSyncing = false;

  /// Start automatic sync (every 30 seconds when online)
  void startAutoSync() {
    _syncTimer?.cancel();
    _syncTimer = Timer.periodic(const Duration(seconds: 30), (_) {
      syncPendingLogs();
    });
  }

  /// Stop automatic sync
  void stopAutoSync() {
    _syncTimer?.cancel();
  }

  /// Sync all PENDING logs to backend
  Future<int> syncPendingLogs() async {
    if (_isSyncing) return 0; // Prevent concurrent syncs
    _isSyncing = true;

    try {
      // Check network connectivity
      final connectivityResult = await Connectivity().checkConnectivity();
      if (connectivityResult == ConnectivityResult.none) {
        print('[SYNC] No network connection - skipping sync');
        return 0;
      }

      // Get all PENDING logs
      final pendingLogs = await _database.getMoodLogsByStatus('PENDING');
      if (pendingLogs.isEmpty) {
        print('[SYNC] No pending logs to sync');
        return 0;
      }

      print('[SYNC] Syncing ${pendingLogs.length} pending logs...');
      int syncedCount = 0;

      for (final log in pendingLogs) {
        try {
          // TODO: Replace with actual API call to your backend
          final serverId = await _uploadToBackend(log);

          // Mark as synced in local database
          await _database.markAsSynced(log.localId, serverId);
          syncedCount++;
          print('[SYNC] ✅ Synced log ${log.localId} -> $serverId');
        } catch (e) {
          print('[SYNC] ❌ Failed to sync log ${log.localId}: $e');
          // Mark as conflict if sync fails repeatedly
          // await _database.markAsConflict(log.localId);
        }
      }

      print('[SYNC] Completed: $syncedCount/${pendingLogs.length} logs synced');
      return syncedCount;
    } finally {
      _isSyncing = false;
    }
  }

  /// Upload mood log to backend API (mock implementation)
  Future<String> _uploadToBackend(MoodLog log) async {
    // TODO: Replace with actual HTTP POST to your backend
    // Example:
    // final response = await http.post(
    //   Uri.parse('https://api.careconnect.com/mood-logs'),
    //   headers: {'Authorization': 'Bearer $token'},
    //   body: jsonEncode({
    //     'local_id': log.localId,
    //     'mood_score': log.moodScore,
    //     'note': log.note,
    //     'patient_id': log.patientId,
    //     'pain_value': log.painValue,
    //     'timestamp': log.lastModified.toIso8601String(),
    //   }),
    // );
    //
    // if (response.statusCode == 201) {
    //   final data = jsonDecode(response.body);
    //   return data['id']; // Server-generated ID
    // } else {
    //   throw Exception('Failed to sync: ${response.statusCode}');
    // }

    // MOCK: Simulate network delay and return fake server ID
    await Future.delayed(const Duration(milliseconds: 500));
    return 'server-${DateTime.now().millisecondsSinceEpoch}';
  }

  /// Force sync now (manual trigger)
  Future<int> forceSyncNow() async {
    print('[SYNC] Manual sync triggered');
    return await syncPendingLogs();
  }
}
