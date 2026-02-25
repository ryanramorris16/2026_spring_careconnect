import 'app_database.dart';
import 'create_table.dart';
import 'offline_table_config.dart';

/// Shared app-level database instance used during startup initialization.
AppDatabase? _startupDb;

/// Ensures configured offline tables exist on platforms supporting `dart:io`.
Future<void> initializeLocalDbOnStartup() async {
  _startupDb ??= AppDatabase();
  final db = _startupDb!;

  for (final tableName in offlineEnabledTables) {
    final createSql = CreateTable.forTable(tableName);
    if (createSql != null && createSql.isNotEmpty) {
      await db.customStatement(createSql);
    }
  }
}
