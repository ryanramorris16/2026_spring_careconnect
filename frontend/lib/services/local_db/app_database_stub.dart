import 'offline_sync_row.dart';

/// Stub data class matching the Drift-generated [Mood] row type on IO.
class Mood {
  const Mood({
    required this.id,
    required this.userId,
    required this.score,
    required this.label,
    required this.createdAt,
  });

  final int id;
  final int userId;
  final int score;
  final String label;
  final DateTime createdAt;
}

class AppDatabase {
  AppDatabase();

  final List<OfflineSyncDbRow> _queue = <OfflineSyncDbRow>[];

  final List<Mood> _moods = <Mood>[];
  int _nextMoodId = 1;

  Future<bool> isEncrypted() async => false;

  Future<int> insertMood({
    required int userId,
    required int score,
    required String label,
    DateTime? createdAt,
  }) async {
    final row = Mood(
      id: _nextMoodId++,
      userId: userId,
      score: score,
      label: label,
      createdAt: createdAt ?? DateTime.now(),
    );
    _moods.add(row);
    return row.id;
  }

  Future<List<Mood>> getMoodsForUser(int userIdValue) async {
    return _moods.where((m) => m.userId == userIdValue).toList();
  }

  Future<List<Mood>> getMoodsForUserOldestFirst(int userIdValue) async {
    final rows = _moods.where((m) => m.userId == userIdValue).toList();
    rows.sort((a, b) => a.createdAt.compareTo(b.createdAt));
    return rows;
  }

  Future<Mood?> getMoodByIdForUser({
    required int moodId,
    required int userIdValue,
  }) async {
    for (final row in _moods) {
      if (row.id == moodId && row.userId == userIdValue) {
        return row;
      }
    }
    return null;
  }

  Future<int> deleteMoodById(int moodId) async {
    final before = _moods.length;
    _moods.removeWhere((m) => m.id == moodId);
    return before == _moods.length ? 0 : 1;
  }

  Future<void> deleteMoodsByIds(Iterable<int> ids) async {
    final set = ids.toSet();
    _moods.removeWhere((m) => set.contains(m.id));
  }

  Future<void> ensureOfflineSyncTable() async {}

  Future<String> upsertOfflineSyncOperation({
    required String id,
    required String method,
    required String url,
    required String headersJson,
    String? bodyJson,
    required String createdAtIso,
    required String fingerprint,
  }) async {
    final existing = _queue.where((item) => item.fingerprint == fingerprint).toList();

    if (existing.isNotEmpty) {
      return existing.first.id;
    }

    _queue.add(
      OfflineSyncDbRow(
        id: id,
        fingerprint: fingerprint,
        method: method,
        url: url,
        headersJson: headersJson,
        bodyJson: bodyJson,
        createdAt: DateTime.tryParse(createdAtIso) ?? DateTime.now().toUtc(),
        status: 'pending',
        retryCount: 0,
        lastError: null,
      ),
    );
    _queue.sort((a, b) => a.createdAt.compareTo(b.createdAt));
    return id;
  }

  Future<List<OfflineSyncDbRow>> getPendingOfflineSyncQueue({
    int limit = 200,
  }) async {
    final rows = _queue.where((item) {
      return item.status == 'pending' ||
          item.status == 'failed' ||
          item.status == 'syncing';
    }).toList()
      ..sort((a, b) => a.createdAt.compareTo(b.createdAt));
    return rows.take(limit).toList();
  }

  Future<int> getPendingOfflineSyncCount() async {
    return _queue
        .where((item) =>
            item.status == 'pending' ||
            item.status == 'failed' ||
            item.status == 'syncing')
        .length;
  }

  Future<OfflineSyncDbRow?> getOfflineSyncById(String id) async {
    for (final item in _queue) {
      if (item.id == id) {
        return item;
      }
    }
    return null;
  }

  Future<void> markOfflineSyncAsSyncing(String id) async {
    final index = _queue.indexWhere((item) => item.id == id);
    if (index < 0) {
      return;
    }
    final item = _queue[index];
    _queue[index] = OfflineSyncDbRow(
      id: item.id,
      fingerprint: item.fingerprint,
      method: item.method,
      url: item.url,
      headersJson: item.headersJson,
      bodyJson: item.bodyJson,
      createdAt: item.createdAt,
      status: 'syncing',
      retryCount: item.retryCount,
      lastError: item.lastError,
    );
  }

  Future<void> markOfflineSyncAsFailed({
    required String id,
    required String errorMessage,
  }) async {
    final index = _queue.indexWhere((item) => item.id == id);
    if (index < 0) {
      return;
    }
    final item = _queue[index];
    _queue[index] = OfflineSyncDbRow(
      id: item.id,
      fingerprint: item.fingerprint,
      method: item.method,
      url: item.url,
      headersJson: item.headersJson,
      bodyJson: item.bodyJson,
      createdAt: item.createdAt,
      status: 'failed',
      retryCount: item.retryCount + 1,
      lastError: errorMessage,
    );
  }

  Future<void> deleteOfflineSyncById(String id) async {
    _queue.removeWhere((item) => item.id == id);
  }

  Future<void> close() async {}

  Future<void> closeDb() async {
    await close();
  }
}
