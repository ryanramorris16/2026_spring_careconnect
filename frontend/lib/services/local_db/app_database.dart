import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqlcipher_flutter_libs/sqlcipher_flutter_libs.dart';
import 'package:sqlite3/open.dart';

import 'db_encryption_service.dart';
import 'generated/jpa_drift_bundle.dart';
import 'offline_sync_row.dart';

part 'app_database.g.dart';

/// Encrypted Drift database used by mobile offline storage.
///
/// Table definitions are generated from backend JPA models and imported from
/// `generated/jpa_drift_bundle.dart`. The generic offline request queue is
/// persisted in a custom `offline_sync` table.
@DriftDatabase(tables: [Moods, Tasks])
class AppDatabase extends _$AppDatabase {
  AppDatabase({DbEncryptionService? encryptionService})
      : _encryptionService = encryptionService ?? DbEncryptionService(),
        super(_openConnection(encryptionService ?? DbEncryptionService()));

  final DbEncryptionService _encryptionService;

  @override
  int get schemaVersion => 1;

  /// Indicates whether an encryption key exists in secure storage.
  Future<bool> isEncrypted() async {
    return _encryptionService.hasEncryptionKey();
  }

  Future<void> ensureOfflineSyncTable() async {
    await customStatement('''
      CREATE TABLE IF NOT EXISTS offline_sync (
        id TEXT PRIMARY KEY,
        method TEXT NOT NULL,
        url TEXT NOT NULL,
        headers_json TEXT NOT NULL,
        body_json TEXT,
        created_at TEXT NOT NULL,
        fingerprint TEXT NOT NULL UNIQUE,
        status TEXT NOT NULL DEFAULT 'pending',
        retry_count INTEGER NOT NULL DEFAULT 0,
        last_error TEXT
      )
    ''');

    await customStatement('''
      CREATE INDEX IF NOT EXISTS idx_offline_sync_status_created_at
      ON offline_sync(status, created_at)
    ''');
  }

  Future<String> upsertOfflineSyncOperation({
    required String id,
    required String method,
    required String url,
    required String headersJson,
    String? bodyJson,
    required String createdAtIso,
    required String fingerprint,
  }) async {
    await ensureOfflineSyncTable();
    await customStatement(
      '''
      INSERT OR IGNORE INTO offline_sync (
        id, method, url, headers_json, body_json, created_at, fingerprint
      ) VALUES (?, ?, ?, ?, ?, ?, ?)
      ''',
      <Object?>[
        id,
        method,
        url,
        headersJson,
        bodyJson,
        createdAtIso,
        fingerprint,
      ],
    );

    final row = await customSelect(
      'SELECT id FROM offline_sync WHERE fingerprint = ? LIMIT 1',
      variables: <Variable<Object>>[Variable.withString(fingerprint)],
    ).getSingleOrNull();

    return row?.read<String>('id') ?? id;
  }

  Future<List<OfflineSyncDbRow>> getPendingOfflineSyncQueue({
    int limit = 200,
  }) async {
    await ensureOfflineSyncTable();
    final rows = await customSelect(
      '''
      SELECT
        id,
        fingerprint,
        method,
        url,
        headers_json,
        body_json,
        created_at,
        status,
        retry_count,
        last_error
      FROM offline_sync
      WHERE status IN ('pending', 'failed', 'syncing')
      ORDER BY created_at ASC, rowid ASC
      LIMIT ?
      ''',
      variables: <Variable<Object>>[Variable.withInt(limit)],
    ).get();

    return rows.map((row) {
      final createdAtRaw = row.read<String>('created_at');
      return OfflineSyncDbRow(
        id: row.read<String>('id'),
        fingerprint: row.read<String>('fingerprint'),
        method: row.read<String>('method'),
        url: row.read<String>('url'),
        headersJson: row.read<String>('headers_json'),
        bodyJson: row.readNullable<String>('body_json'),
        createdAt: DateTime.tryParse(createdAtRaw) ?? DateTime.now().toUtc(),
        status: row.read<String>('status'),
        retryCount: row.read<int>('retry_count'),
        lastError: row.readNullable<String>('last_error'),
      );
    }).toList();
  }

  Future<int> getPendingOfflineSyncCount() async {
    await ensureOfflineSyncTable();
    final row = await customSelect(
      '''
      SELECT COUNT(*) AS count
      FROM offline_sync
      WHERE status IN ('pending', 'failed', 'syncing')
      ''',
    ).getSingle();
    return row.read<int>('count');
  }

  Future<OfflineSyncDbRow?> getOfflineSyncById(String id) async {
    await ensureOfflineSyncTable();
    final row = await customSelect(
      '''
      SELECT
        id,
        fingerprint,
        method,
        url,
        headers_json,
        body_json,
        created_at,
        status,
        retry_count,
        last_error
      FROM offline_sync
      WHERE id = ?
      LIMIT 1
      ''',
      variables: <Variable<Object>>[Variable.withString(id)],
    ).getSingleOrNull();

    if (row == null) {
      return null;
    }

    final createdAtRaw = row.read<String>('created_at');
    return OfflineSyncDbRow(
      id: row.read<String>('id'),
      fingerprint: row.read<String>('fingerprint'),
      method: row.read<String>('method'),
      url: row.read<String>('url'),
      headersJson: row.read<String>('headers_json'),
      bodyJson: row.readNullable<String>('body_json'),
      createdAt: DateTime.tryParse(createdAtRaw) ?? DateTime.now().toUtc(),
      status: row.read<String>('status'),
      retryCount: row.read<int>('retry_count'),
      lastError: row.readNullable<String>('last_error'),
    );
  }

  Future<void> markOfflineSyncAsSyncing(String id) async {
    await customStatement(
      "UPDATE offline_sync SET status = 'syncing' WHERE id = ?",
      <Object?>[id],
    );
  }

  Future<void> markOfflineSyncAsFailed({
    required String id,
    required String errorMessage,
  }) async {
    await customStatement(
      '''
      UPDATE offline_sync
      SET
        status = 'failed',
        retry_count = retry_count + 1,
        last_error = ?
      WHERE id = ?
      ''',
      <Object?>[errorMessage, id],
    );
  }

  Future<void> deleteOfflineSyncById(String id) async {
    await customStatement(
      'DELETE FROM offline_sync WHERE id = ?',
      <Object?>[id],
    );
  }

  Future<void> closeDb() async {
    await close();
  }
}

/// Opens the encrypted sqlite database and applies SQLCipher keying.
QueryExecutor _openConnection(DbEncryptionService encryptionService) {
  return LazyDatabase(() async {
    // SQLCipher requires overriding sqlite loader on Android.
    if (Platform.isAndroid) {
      open.overrideFor(OperatingSystem.android, openCipherOnAndroid);
      await applyWorkaroundToOpenSqlCipherOnOldAndroidVersions();
    }

    final dir = await getApplicationDocumentsDirectory();
    final file = File(p.join(dir.path, 'careconnect_mobile.sqlite'));
    final encryptionKey = await encryptionService.getOrCreateEncryptionKey();

    return NativeDatabase(
      file,
      setup: (database) {
        final escaped = encryptionService.escapeForPragma(encryptionKey);
        database.execute("PRAGMA key = '$escaped';");
        database.execute('PRAGMA foreign_keys = ON;');
      },
    );
  });
}
