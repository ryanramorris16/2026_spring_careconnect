// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'app_database.dart';

// ignore_for_file: type=lint
class $MoodsTable extends Moods with TableInfo<$MoodsTable, Mood> {
  @override
  final GeneratedDatabase attachedDatabase;
  final String? _alias;
  $MoodsTable(this.attachedDatabase, [this._alias]);
  static const VerificationMeta _idMeta = const VerificationMeta('id');
  @override
  late final GeneratedColumn<int> id = GeneratedColumn<int>(
    'id',
    aliasedName,
    false,
    hasAutoIncrement: true,
    type: DriftSqlType.int,
    requiredDuringInsert: false,
    defaultConstraints: GeneratedColumn.constraintIsAlways(
      'PRIMARY KEY AUTOINCREMENT',
    ),
  );
  static const VerificationMeta _userIdMeta = const VerificationMeta('userId');
  @override
  late final GeneratedColumn<int> userId = GeneratedColumn<int>(
    'user_id',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _scoreMeta = const VerificationMeta('score');
  @override
  late final GeneratedColumn<int> score = GeneratedColumn<int>(
    'score',
    aliasedName,
    false,
    type: DriftSqlType.int,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _labelMeta = const VerificationMeta('label');
  @override
  late final GeneratedColumn<String> label = GeneratedColumn<String>(
    'label',
    aliasedName,
    false,
    type: DriftSqlType.string,
    requiredDuringInsert: true,
  );
  static const VerificationMeta _createdAtMeta = const VerificationMeta(
    'createdAt',
  );
  @override
  late final GeneratedColumn<DateTime> createdAt = GeneratedColumn<DateTime>(
    'created_at',
    aliasedName,
    false,
    type: DriftSqlType.dateTime,
    requiredDuringInsert: false,
    defaultValue: currentDateAndTime,
  );
  @override
  List<GeneratedColumn> get $columns => [id, userId, score, label, createdAt];
  @override
  String get aliasedName => _alias ?? actualTableName;
  @override
  String get actualTableName => $name;
  static const String $name = 'moods';
  @override
  VerificationContext validateIntegrity(
    Insertable<Mood> instance, {
    bool isInserting = false,
  }) {
    final context = VerificationContext();
    final data = instance.toColumns(true);
    if (data.containsKey('id')) {
      context.handle(_idMeta, id.isAcceptableOrUnknown(data['id']!, _idMeta));
    }
    if (data.containsKey('user_id')) {
      context.handle(
        _userIdMeta,
        userId.isAcceptableOrUnknown(data['user_id']!, _userIdMeta),
      );
    } else if (isInserting) {
      context.missing(_userIdMeta);
    }
    if (data.containsKey('score')) {
      context.handle(
        _scoreMeta,
        score.isAcceptableOrUnknown(data['score']!, _scoreMeta),
      );
    } else if (isInserting) {
      context.missing(_scoreMeta);
    }
    if (data.containsKey('label')) {
      context.handle(
        _labelMeta,
        label.isAcceptableOrUnknown(data['label']!, _labelMeta),
      );
    } else if (isInserting) {
      context.missing(_labelMeta);
    }
    if (data.containsKey('created_at')) {
      context.handle(
        _createdAtMeta,
        createdAt.isAcceptableOrUnknown(data['created_at']!, _createdAtMeta),
      );
    }
    return context;
  }

  @override
  Set<GeneratedColumn> get $primaryKey => {id};
  @override
  Mood map(Map<String, dynamic> data, {String? tablePrefix}) {
    final effectivePrefix = tablePrefix != null ? '$tablePrefix.' : '';
    return Mood(
      id: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}id'],
      )!,
      userId: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}user_id'],
      )!,
      score: attachedDatabase.typeMapping.read(
        DriftSqlType.int,
        data['${effectivePrefix}score'],
      )!,
      label: attachedDatabase.typeMapping.read(
        DriftSqlType.string,
        data['${effectivePrefix}label'],
      )!,
      createdAt: attachedDatabase.typeMapping.read(
        DriftSqlType.dateTime,
        data['${effectivePrefix}created_at'],
      )!,
    );
  }

  @override
  $MoodsTable createAlias(String alias) {
    return $MoodsTable(attachedDatabase, alias);
  }
}

class Mood extends DataClass implements Insertable<Mood> {
  final int id;
  final int userId;
  final int score;
  final String label;
  final DateTime createdAt;
  const Mood({
    required this.id,
    required this.userId,
    required this.score,
    required this.label,
    required this.createdAt,
  });
  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    map['id'] = Variable<int>(id);
    map['user_id'] = Variable<int>(userId);
    map['score'] = Variable<int>(score);
    map['label'] = Variable<String>(label);
    map['created_at'] = Variable<DateTime>(createdAt);
    return map;
  }

  MoodsCompanion toCompanion(bool nullToAbsent) {
    return MoodsCompanion(
      id: Value(id),
      userId: Value(userId),
      score: Value(score),
      label: Value(label),
      createdAt: Value(createdAt),
    );
  }

  factory Mood.fromJson(
    Map<String, dynamic> json, {
    ValueSerializer? serializer,
  }) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return Mood(
      id: serializer.fromJson<int>(json['id']),
      userId: serializer.fromJson<int>(json['userId']),
      score: serializer.fromJson<int>(json['score']),
      label: serializer.fromJson<String>(json['label']),
      createdAt: serializer.fromJson<DateTime>(json['createdAt']),
    );
  }
  @override
  Map<String, dynamic> toJson({ValueSerializer? serializer}) {
    serializer ??= driftRuntimeOptions.defaultSerializer;
    return <String, dynamic>{
      'id': serializer.toJson<int>(id),
      'userId': serializer.toJson<int>(userId),
      'score': serializer.toJson<int>(score),
      'label': serializer.toJson<String>(label),
      'createdAt': serializer.toJson<DateTime>(createdAt),
    };
  }

  Mood copyWith({
    int? id,
    int? userId,
    int? score,
    String? label,
    DateTime? createdAt,
  }) => Mood(
    id: id ?? this.id,
    userId: userId ?? this.userId,
    score: score ?? this.score,
    label: label ?? this.label,
    createdAt: createdAt ?? this.createdAt,
  );
  Mood copyWithCompanion(MoodsCompanion data) {
    return Mood(
      id: data.id.present ? data.id.value : this.id,
      userId: data.userId.present ? data.userId.value : this.userId,
      score: data.score.present ? data.score.value : this.score,
      label: data.label.present ? data.label.value : this.label,
      createdAt: data.createdAt.present ? data.createdAt.value : this.createdAt,
    );
  }

  @override
  String toString() {
    return (StringBuffer('Mood(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('score: $score, ')
          ..write('label: $label, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }

  @override
  int get hashCode => Object.hash(id, userId, score, label, createdAt);
  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      (other is Mood &&
          other.id == this.id &&
          other.userId == this.userId &&
          other.score == this.score &&
          other.label == this.label &&
          other.createdAt == this.createdAt);
}

class MoodsCompanion extends UpdateCompanion<Mood> {
  final Value<int> id;
  final Value<int> userId;
  final Value<int> score;
  final Value<String> label;
  final Value<DateTime> createdAt;
  const MoodsCompanion({
    this.id = const Value.absent(),
    this.userId = const Value.absent(),
    this.score = const Value.absent(),
    this.label = const Value.absent(),
    this.createdAt = const Value.absent(),
  });
  MoodsCompanion.insert({
    this.id = const Value.absent(),
    required int userId,
    required int score,
    required String label,
    this.createdAt = const Value.absent(),
  }) : userId = Value(userId),
       score = Value(score),
       label = Value(label);
  static Insertable<Mood> custom({
    Expression<int>? id,
    Expression<int>? userId,
    Expression<int>? score,
    Expression<String>? label,
    Expression<DateTime>? createdAt,
  }) {
    return RawValuesInsertable({
      if (id != null) 'id': id,
      if (userId != null) 'user_id': userId,
      if (score != null) 'score': score,
      if (label != null) 'label': label,
      if (createdAt != null) 'created_at': createdAt,
    });
  }

  MoodsCompanion copyWith({
    Value<int>? id,
    Value<int>? userId,
    Value<int>? score,
    Value<String>? label,
    Value<DateTime>? createdAt,
  }) {
    return MoodsCompanion(
      id: id ?? this.id,
      userId: userId ?? this.userId,
      score: score ?? this.score,
      label: label ?? this.label,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  @override
  Map<String, Expression> toColumns(bool nullToAbsent) {
    final map = <String, Expression>{};
    if (id.present) {
      map['id'] = Variable<int>(id.value);
    }
    if (userId.present) {
      map['user_id'] = Variable<int>(userId.value);
    }
    if (score.present) {
      map['score'] = Variable<int>(score.value);
    }
    if (label.present) {
      map['label'] = Variable<String>(label.value);
    }
    if (createdAt.present) {
      map['created_at'] = Variable<DateTime>(createdAt.value);
    }
    return map;
  }

  @override
  String toString() {
    return (StringBuffer('MoodsCompanion(')
          ..write('id: $id, ')
          ..write('userId: $userId, ')
          ..write('score: $score, ')
          ..write('label: $label, ')
          ..write('createdAt: $createdAt')
          ..write(')'))
        .toString();
  }
}

abstract class _$AppDatabase extends GeneratedDatabase {
  _$AppDatabase(QueryExecutor e) : super(e);
  $AppDatabaseManager get managers => $AppDatabaseManager(this);
  late final $MoodsTable moods = $MoodsTable(this);
  @override
  Iterable<TableInfo<Table, Object?>> get allTables =>
      allSchemaEntities.whereType<TableInfo<Table, Object?>>();
  @override
  List<DatabaseSchemaEntity> get allSchemaEntities => [moods];
}

typedef $$MoodsTableCreateCompanionBuilder =
    MoodsCompanion Function({
      Value<int> id,
      required int userId,
      required int score,
      required String label,
      Value<DateTime> createdAt,
    });
typedef $$MoodsTableUpdateCompanionBuilder =
    MoodsCompanion Function({
      Value<int> id,
      Value<int> userId,
      Value<int> score,
      Value<String> label,
      Value<DateTime> createdAt,
    });

class $$MoodsTableFilterComposer extends Composer<_$AppDatabase, $MoodsTable> {
  $$MoodsTableFilterComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnFilters<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get userId => $composableBuilder(
    column: $table.userId,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<int> get score => $composableBuilder(
    column: $table.score,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<String> get label => $composableBuilder(
    column: $table.label,
    builder: (column) => ColumnFilters(column),
  );

  ColumnFilters<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnFilters(column),
  );
}

class $$MoodsTableOrderingComposer
    extends Composer<_$AppDatabase, $MoodsTable> {
  $$MoodsTableOrderingComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  ColumnOrderings<int> get id => $composableBuilder(
    column: $table.id,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get userId => $composableBuilder(
    column: $table.userId,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<int> get score => $composableBuilder(
    column: $table.score,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<String> get label => $composableBuilder(
    column: $table.label,
    builder: (column) => ColumnOrderings(column),
  );

  ColumnOrderings<DateTime> get createdAt => $composableBuilder(
    column: $table.createdAt,
    builder: (column) => ColumnOrderings(column),
  );
}

class $$MoodsTableAnnotationComposer
    extends Composer<_$AppDatabase, $MoodsTable> {
  $$MoodsTableAnnotationComposer({
    required super.$db,
    required super.$table,
    super.joinBuilder,
    super.$addJoinBuilderToRootComposer,
    super.$removeJoinBuilderFromRootComposer,
  });
  GeneratedColumn<int> get id =>
      $composableBuilder(column: $table.id, builder: (column) => column);

  GeneratedColumn<int> get userId =>
      $composableBuilder(column: $table.userId, builder: (column) => column);

  GeneratedColumn<int> get score =>
      $composableBuilder(column: $table.score, builder: (column) => column);

  GeneratedColumn<String> get label =>
      $composableBuilder(column: $table.label, builder: (column) => column);

  GeneratedColumn<DateTime> get createdAt =>
      $composableBuilder(column: $table.createdAt, builder: (column) => column);
}

class $$MoodsTableTableManager
    extends
        RootTableManager<
          _$AppDatabase,
          $MoodsTable,
          Mood,
          $$MoodsTableFilterComposer,
          $$MoodsTableOrderingComposer,
          $$MoodsTableAnnotationComposer,
          $$MoodsTableCreateCompanionBuilder,
          $$MoodsTableUpdateCompanionBuilder,
          (Mood, BaseReferences<_$AppDatabase, $MoodsTable, Mood>),
          Mood,
          PrefetchHooks Function()
        > {
  $$MoodsTableTableManager(_$AppDatabase db, $MoodsTable table)
    : super(
        TableManagerState(
          db: db,
          table: table,
          createFilteringComposer: () =>
              $$MoodsTableFilterComposer($db: db, $table: table),
          createOrderingComposer: () =>
              $$MoodsTableOrderingComposer($db: db, $table: table),
          createComputedFieldComposer: () =>
              $$MoodsTableAnnotationComposer($db: db, $table: table),
          updateCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                Value<int> userId = const Value.absent(),
                Value<int> score = const Value.absent(),
                Value<String> label = const Value.absent(),
                Value<DateTime> createdAt = const Value.absent(),
              }) => MoodsCompanion(
                id: id,
                userId: userId,
                score: score,
                label: label,
                createdAt: createdAt,
              ),
          createCompanionCallback:
              ({
                Value<int> id = const Value.absent(),
                required int userId,
                required int score,
                required String label,
                Value<DateTime> createdAt = const Value.absent(),
              }) => MoodsCompanion.insert(
                id: id,
                userId: userId,
                score: score,
                label: label,
                createdAt: createdAt,
              ),
          withReferenceMapper: (p0) => p0
              .map((e) => (e.readTable(table), BaseReferences(db, table, e)))
              .toList(),
          prefetchHooksCallback: null,
        ),
      );
}

typedef $$MoodsTableProcessedTableManager =
    ProcessedTableManager<
      _$AppDatabase,
      $MoodsTable,
      Mood,
      $$MoodsTableFilterComposer,
      $$MoodsTableOrderingComposer,
      $$MoodsTableAnnotationComposer,
      $$MoodsTableCreateCompanionBuilder,
      $$MoodsTableUpdateCompanionBuilder,
      (Mood, BaseReferences<_$AppDatabase, $MoodsTable, Mood>),
      Mood,
      PrefetchHooks Function()
    >;

class $AppDatabaseManager {
  final _$AppDatabase _db;
  $AppDatabaseManager(this._db);
  $$MoodsTableTableManager get moods =>
      $$MoodsTableTableManager(_db, _db.moods);
}
