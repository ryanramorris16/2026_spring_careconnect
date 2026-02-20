import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:uuid/uuid.dart';

/// Mood log model matching Team-B TDD v3 schema
class MoodLog {
  final String localId;
  final int moodScore;
  final String? note;
  final int patientId;
  final int? painValue;
  final String? serverId;
  final String syncStatus;
  final DateTime lastModified;

  MoodLog({
    required this.localId,
    required this.moodScore,
    this.note,
    required this.patientId,
    this.painValue,
    this.serverId,
    required this.syncStatus,
    required this.lastModified,
  });

  /// Convert database row to MoodLog object
  factory MoodLog.fromMap(Map<String, dynamic> map) {
    return MoodLog(
      localId: map['local_id'] as String,
      moodScore: map['mood_score'] as int,
      note: map['note'] as String?,
      patientId: map['patient_id'] as int,
      painValue: map['pain_value'] as int?,
      serverId: map['server_id'] as String?,
      syncStatus: map['sync_status'] as String,
      lastModified: DateTime.parse(map['last_modified'] as String),
    );
  }

  /// Convert MoodLog object to database row
  Map<String, dynamic> toMap() {
    return {
      'local_id': localId,
      'mood_score': moodScore,
      'note': note,
      'patient_id': patientId,
      'pain_value': painValue,
      'server_id': serverId,
      'sync_status': syncStatus,
      'last_modified': lastModified.toIso8601String(),
    };
  }
}

/// SQLite database for offline mood tracking (BNS-5)
class AppDatabase {
  static final AppDatabase _instance = AppDatabase._internal();
  factory AppDatabase() => _instance;
  AppDatabase._internal();

  static Database? _database;

  /// Get database instance (lazy initialization)
  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  /// Initialize SQLite database
  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'careconnect_offline.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE mood_logs (
            local_id TEXT PRIMARY KEY,
            mood_score INTEGER NOT NULL CHECK(mood_score >= 1 AND mood_score <= 5),
            note TEXT,
            patient_id INTEGER NOT NULL,
            pain_value INTEGER,
            server_id TEXT,
            sync_status TEXT NOT NULL DEFAULT 'PENDING',
            last_modified TEXT NOT NULL
          )
        ''');
      },
    );
  }

  /// Get all mood logs
  Future<List<MoodLog>> getAllMoodLogs() async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      'mood_logs',
      orderBy: 'last_modified DESC',
    );
    return List.generate(maps.length, (i) => MoodLog.fromMap(maps[i]));
  }

  /// Get mood logs by sync status
  Future<List<MoodLog>> getMoodLogsByStatus(String status) async {
    final db = await database;
    final List<Map<String, dynamic>> maps = await db.query(
      'mood_logs',
      where: 'sync_status = ?',
      whereArgs: [status],
      orderBy: 'last_modified DESC',
    );
    return List.generate(maps.length, (i) => MoodLog.fromMap(maps[i]));
  }

  /// Insert a new mood log
  Future<void> insertMoodLog({
    required int moodScore,
    String? note,
    required int patientId,
    int? painValue,
  }) async {
    final db = await database;
    final log = MoodLog(
      localId: const Uuid().v4(),
      moodScore: moodScore,
      note: note,
      patientId: patientId,
      painValue: painValue,
      serverId: null,
      syncStatus: 'PENDING',
      lastModified: DateTime.now().toUtc(),
    );
    await db.insert('mood_logs', log.toMap());
  }

  /// Update a mood log
  Future<void> updateMoodLog(MoodLog log) async {
    final db = await database;
    await db.update(
      'mood_logs',
      log.toMap(),
      where: 'local_id = ?',
      whereArgs: [log.localId],
    );
  }

  /// Delete a mood log
  Future<void> deleteMoodLog(String localId) async {
    final db = await database;
    await db.delete('mood_logs', where: 'local_id = ?', whereArgs: [localId]);
  }

  /// Mark mood log as synced with server
  Future<void> markAsSynced(String localId, String serverId) async {
    final db = await database;
    await db.update(
      'mood_logs',
      {
        'server_id': serverId,
        'sync_status': 'SYNCED',
        'last_modified': DateTime.now().toUtc().toIso8601String(),
      },
      where: 'local_id = ?',
      whereArgs: [localId],
    );
  }

  /// Mark mood log as having sync conflict
  Future<void> markAsConflict(String localId) async {
    final db = await database;
    await db.update(
      'mood_logs',
      {'sync_status': 'CONFLICT'},
      where: 'local_id = ?',
      whereArgs: [localId],
    );
  }

  /// Close database connection
  Future<void> close() async {
    final db = await database;
    await db.close();
  }

  /// Clear all mood logs (for testing)
  Future<void> clearAllLogs() async {
    final db = await database;
    await db.delete('mood_logs');
  }
}
