import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:sqlcipher_flutter_libs/sqlcipher_flutter_libs.dart';
import 'package:sqlite3/open.dart';

import 'db_encryption_service.dart';

part 'app_database.g.dart';

/// Local copy of backend `Mood` table for offline mode.
///
/// Source model: backend/core/src/main/java/com/careconnect/model/Mood.java
class Moods extends Table {
  IntColumn get id => integer().autoIncrement()();
  IntColumn get userId => integer()();
  IntColumn get score => integer()();
  TextColumn get label => text()();
  DateTimeColumn get createdAt =>
      dateTime().withDefault(currentDateAndTime)();
}

/// Encrypted Drift database used by mobile offline storage.
@DriftDatabase(tables: [Moods])
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

  /// Inserts one mood row into the local encrypted database.
  Future<int> insertMood({
    required int userId,
    required int score,
    required String label,
    DateTime? createdAt,
  }) {
    return into(moods).insert(
      MoodsCompanion.insert(
        userId: userId,
        score: score,
        label: label,
        createdAt: Value(createdAt ?? DateTime.now()),
      ),
    );
  }

  /// Returns moods for one user, newest first.
  Future<List<Mood>> getMoodsForUser(int userIdValue) {
    final query = select(moods)
      ..where((tbl) => tbl.userId.equals(userIdValue))
      ..orderBy([(tbl) => OrderingTerm.desc(tbl.createdAt)]);
    return query.get();
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
