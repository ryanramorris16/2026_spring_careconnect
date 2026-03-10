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

/// Stub AppDatabase for web. All methods are no-ops.
class AppDatabase {
  AppDatabase();

  Future<bool> isEncrypted() async => false;

  Future<int> insertMood({
    required int userId,
    required int score,
    required String label,
    DateTime? createdAt,
  }) async => 0;

  Future<List<Mood>> getMoodsForUser(int userIdValue) async => [];

  Future<List<Mood>> getMoodsForUserOldestFirst(int userIdValue) async => [];

  Future<Mood?> getMoodByIdForUser({
    required int moodId,
    required int userIdValue,
  }) async => null;

  Future<int> deleteMoodById(int moodId) async => 0;

  Future<void> deleteMoodsByIds(Iterable<int> ids) async {}

  Future<void> close() async {}
}
